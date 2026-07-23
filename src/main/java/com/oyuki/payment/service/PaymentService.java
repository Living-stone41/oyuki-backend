package com.oyuki.payment.service;

import com.oyuki.notification.enums.NotificationType;
import com.oyuki.notification.service.NotificationService;
import com.oyuki.order.entity.Order;
import com.oyuki.order.entity.OrderItem;
import com.oyuki.order.enums.PaymentMethod;
import com.oyuki.order.enums.PaymentStatus;
import com.oyuki.order.repository.OrderItemRepository;
import com.oyuki.order.repository.OrderRepository;
import com.oyuki.payment.dto.BankAccountRequest;
import com.oyuki.payment.dto.ConfirmPaymentProofRequest;
import com.oyuki.payment.dto.PaymentProofResponse;
import com.oyuki.payment.dto.PlatformBankAccountResponse;
import com.oyuki.payment.dto.RejectPaymentProofRequest;
import com.oyuki.payment.entity.PaymentProof;
import com.oyuki.payment.entity.PlatformBankAccount;
import com.oyuki.payment.enums.PaymentProofStatus;
import com.oyuki.payment.repository.PaymentProofRepository;
import com.oyuki.payment.repository.PlatformBankAccountRepository;
import com.oyuki.user.entity.User;
import com.oyuki.user.enums.AccountStatus;
import com.oyuki.user.enums.Role;
import com.oyuki.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private static final long MAX_RECEIPT_SIZE =
            5L * 1024L * 1024L;

    private static final Set<String>
            ALLOWED_CONTENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp",
                    "application/pdf"
            );

    private final PlatformBankAccountRepository
            bankAccountRepository;

    private final PaymentProofRepository
            paymentProofRepository;

    private final OrderRepository orderRepository;

    private final OrderItemRepository
            orderItemRepository;

    private final UserRepository userRepository;

    private final NotificationService
            notificationService;

    private final PaymentReceiptEmailService
            paymentReceiptEmailService;

    private final Path receiptUploadDirectory;

    public PaymentService(
            PlatformBankAccountRepository bankAccountRepository,
            PaymentProofRepository paymentProofRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            PaymentReceiptEmailService paymentReceiptEmailService,

            @Value(
                    "${app.upload.payment-receipts:uploads/payment-receipts}"
            )
            String receiptUploadDirectory
    ) {
        this.bankAccountRepository =
                bankAccountRepository;

        this.paymentProofRepository =
                paymentProofRepository;

        this.orderRepository =
                orderRepository;

        this.orderItemRepository =
                orderItemRepository;

        this.userRepository =
                userRepository;

        this.notificationService =
                notificationService;

        this.paymentReceiptEmailService =
                paymentReceiptEmailService;

        this.receiptUploadDirectory =
                Paths.get(receiptUploadDirectory)
                        .toAbsolutePath()
                        .normalize();
    }

    /*
     * =========================================================
     * ACTIVE OYUKI BANK ACCOUNT
     * =========================================================
     */

    @Transactional(readOnly = true)
    public PlatformBankAccountResponse
    getActiveBankAccount() {

        PlatformBankAccount account =
                bankAccountRepository
                        .findFirstByActiveTrueOrderByUpdatedAtDesc()
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Oyuki bank account has not been configured"
                                )
                        );

        return PlatformBankAccountResponse.from(
                account
        );
    }

    /*
     * Admin creates a new active bank account.
     *
     * Previous active accounts are automatically
     * deactivated.
     */
    @Transactional
    public PlatformBankAccountResponse
    createBankAccount(
            Long adminId,
            BankAccountRequest request
    ) {
        User admin =
                getActiveAdmin(adminId);

        validateBankAccountRequest(request);

        List<PlatformBankAccount> activeAccounts =
                bankAccountRepository
                        .findAllByActiveTrue();

        for (
                PlatformBankAccount account
                : activeAccounts
        ) {
            account.setActive(false);
        }

        bankAccountRepository.saveAll(
                activeAccounts
        );

        PlatformBankAccount account =
                PlatformBankAccount.builder()
                        .bankName(
                                request.bankName()
                                        .trim()
                        )
                        .accountName(
                                request.accountName()
                                        .trim()
                        )
                        .accountNumber(
                                request.accountNumber()
                                        .replace(" ", "")
                                        .trim()
                        )
                        .paymentInstructions(
                                clean(
                                        request.paymentInstructions()
                                )
                        )
                        .active(true)
                        .createdBy(admin)
                        .build();

        PlatformBankAccount savedAccount =
                bankAccountRepository.save(
                        account
                );

        return PlatformBankAccountResponse.from(
                savedAccount
        );
    }

    @Transactional(readOnly = true)
    public List<PlatformBankAccountResponse>
    getAllBankAccounts(
            Long adminId
    ) {
        getActiveAdmin(adminId);

        return bankAccountRepository
                .findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(
                        PlatformBankAccountResponse::from
                )
                .toList();
    }

    @Transactional
    public PlatformBankAccountResponse
    activateBankAccount(
            Long adminId,
            Long bankAccountId
    ) {
        getActiveAdmin(adminId);

        PlatformBankAccount selectedAccount =
                bankAccountRepository
                        .findById(bankAccountId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Bank account not found"
                                )
                        );

        List<PlatformBankAccount> activeAccounts =
                bankAccountRepository
                        .findAllByActiveTrue();

        for (
                PlatformBankAccount account
                : activeAccounts
        ) {
            account.setActive(false);
        }

        bankAccountRepository.saveAll(
                activeAccounts
        );

        selectedAccount.setActive(true);

        PlatformBankAccount savedAccount =
                bankAccountRepository.save(
                        selectedAccount
                );

        return PlatformBankAccountResponse.from(
                savedAccount
        );
    }

    /*
     * =========================================================
     * CUSTOMER UPLOADS PAYMENT RECEIPT
     * =========================================================
     */

    @Transactional
    public PaymentProofResponse uploadPaymentProof(
            Long customerId,
            Long orderId,
            BigDecimal amount,
            String senderBankName,
            String senderAccountName,
            String transactionReference,
            LocalDateTime paymentDate,
            MultipartFile receiptFile
    ) {
        User customer =
                getActiveCustomer(customerId);

        Order order =
                orderRepository
                        .findByIdAndCustomer_Id(
                                orderId,
                                customerId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Order not found"
                                )
                        );

        validatePaymentProof(
                order,
                amount,
                senderBankName,
                senderAccountName,
                transactionReference,
                paymentDate,
                receiptFile
        );

        boolean activeProofExists =
                paymentProofRepository
                        .existsByOrder_IdAndStatusIn(
                                orderId,
                                List.of(
                                        PaymentProofStatus.SUBMITTED,
                                        PaymentProofStatus.CONFIRMED
                                )
                        );

        if (activeProofExists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order already has a payment receipt awaiting review or already confirmed"
            );
        }

        if (
                paymentProofRepository
                        .existsByTransactionReferenceIgnoreCase(
                                transactionReference.trim()
                        )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This transaction reference has already been submitted"
            );
        }

        String storedReceiptPath =
                saveReceiptFile(
                        receiptFile,
                        order.getOrderNumber()
                );

        PaymentProof paymentProof =
                PaymentProof.builder()
                        .order(order)
                        .customer(customer)
                        .amount(amount)
                        .senderBankName(
                                senderBankName.trim()
                        )
                        .senderAccountName(
                                senderAccountName.trim()
                        )
                        .transactionReference(
                                transactionReference.trim()
                        )
                        .paymentDate(paymentDate)
                        .receiptUrl(
                                storedReceiptPath
                        )
                        .originalFileName(
                                receiptFile
                                        .getOriginalFilename()
                        )
                        .fileContentType(
                                receiptFile.getContentType()
                        )
                        .fileSize(
                                receiptFile.getSize()
                        )
                        .status(
                                PaymentProofStatus.SUBMITTED
                        )
                        .build();

        PaymentProof savedProof =
                paymentProofRepository.save(
                        paymentProof
                );

        order.setPaymentStatus(
                PaymentStatus.AWAITING_CONFIRMATION
        );

        orderRepository.save(order);

        sendProofSubmittedNotifications(
                savedProof
        );

        paymentReceiptEmailService
                .sendReceiptSubmitted(
                        savedProof
                );

        return PaymentProofResponse.from(
                savedProof
        );
    }

    /*
     * =========================================================
     * CUSTOMER PAYMENT HISTORY
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<PaymentProofResponse>
    getMyPaymentProofs(
            Long customerId
    ) {
        getActiveCustomer(customerId);

        return paymentProofRepository
                .findAllByCustomer_IdOrderByCreatedAtDesc(
                        customerId
                )
                .stream()
                .map(PaymentProofResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentProofResponse>
    getMyOrderPaymentProofs(
            Long customerId,
            Long orderId
    ) {
        getActiveCustomer(customerId);

        orderRepository
                .findByIdAndCustomer_Id(
                        orderId,
                        customerId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order not found"
                        )
                );

        return paymentProofRepository
                .findAllByOrder_IdOrderByCreatedAtDesc(
                        orderId
                )
                .stream()
                .map(PaymentProofResponse::from)
                .toList();
    }

    /*
     * =========================================================
     * ADMIN PAYMENT REVIEW
     * =========================================================
     */

    @Transactional(readOnly = true)
    public List<PaymentProofResponse>
    getAllPaymentProofs(
            Long adminId,
            PaymentProofStatus status
    ) {
        getActiveAdmin(adminId);

        List<PaymentProof> paymentProofs;

        if (status == null) {
            paymentProofs =
                    paymentProofRepository
                            .findAllByOrderByCreatedAtDesc();
        } else {
            paymentProofs =
                    paymentProofRepository
                            .findAllByStatusOrderByCreatedAtDesc(
                                    status
                            );
        }

        return paymentProofs.stream()
                .map(PaymentProofResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentProofResponse getAdminPaymentProof(
            Long adminId,
            Long paymentProofId
    ) {
        getActiveAdmin(adminId);

        PaymentProof paymentProof =
                getPaymentProof(
                        paymentProofId
                );

        return PaymentProofResponse.from(
                paymentProof
        );
    }

    @Transactional
    public PaymentProofResponse confirmPaymentProof(
            Long adminId,
            Long paymentProofId,
            ConfirmPaymentProofRequest request
    ) {
        User admin =
                getActiveAdmin(adminId);

        PaymentProof paymentProof =
                getPaymentProof(
                        paymentProofId
                );

        if (
                paymentProof.getStatus()
                        != PaymentProofStatus.SUBMITTED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only submitted payment receipts can be confirmed"
            );
        }

        Order order =
                paymentProof.getOrder();

        paymentProof.setStatus(
                PaymentProofStatus.CONFIRMED
        );

        paymentProof.setReviewedBy(admin);

        paymentProof.setAdminNote(
                request == null
                        ? null
                        : clean(request.note())
        );

        paymentProof.setRejectionReason(null);

        paymentProof.setReviewedAt(
                LocalDateTime.now()
        );

        PaymentProof savedProof =
                paymentProofRepository.save(
                        paymentProof
                );

        order.setPaymentStatus(
                PaymentStatus.PAID
        );

        orderRepository.save(order);

        sendPaymentConfirmedNotifications(
                savedProof
        );

        return PaymentProofResponse.from(
                savedProof
        );
    }

    @Transactional
    public PaymentProofResponse rejectPaymentProof(
            Long adminId,
            Long paymentProofId,
            RejectPaymentProofRequest request
    ) {
        User admin =
                getActiveAdmin(adminId);

        PaymentProof paymentProof =
                getPaymentProof(
                        paymentProofId
                );

        if (
                paymentProof.getStatus()
                        != PaymentProofStatus.SUBMITTED
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only submitted payment receipts can be rejected"
            );
        }

        if (
                request == null ||
                request.reason() == null ||
                request.reason().isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment rejection reason is required"
            );
        }

        paymentProof.setStatus(
                PaymentProofStatus.REJECTED
        );

        paymentProof.setReviewedBy(admin);

        paymentProof.setRejectionReason(
                request.reason().trim()
        );

        paymentProof.setAdminNote(null);

        paymentProof.setReviewedAt(
                LocalDateTime.now()
        );

        PaymentProof savedProof =
                paymentProofRepository.save(
                        paymentProof
                );

        Order order =
                paymentProof.getOrder();

        order.setPaymentStatus(
                PaymentStatus.REJECTED
        );

        orderRepository.save(order);

        notificationService.sendNotification(
                order.getCustomer(),
                NotificationType.PAYMENT_REJECTED,
                "Payment receipt rejected",
                "The payment receipt for order "
                        + order.getOrderNumber()
                        + " was rejected. Reason: "
                        + request.reason().trim(),
                "PAYMENT_PROOF",
                savedProof.getId(),
                "/customer/orders/"
                        + order.getId(),
                null
        );

        return PaymentProofResponse.from(
                savedProof
        );
    }

    /*
     * =========================================================
     * RECEIPT FILE DOWNLOAD
     * =========================================================
     */

    @Transactional(readOnly = true)
    public ReceiptDownload getCustomerReceipt(
            Long customerId,
            Long paymentProofId
    ) {
        getActiveCustomer(customerId);

        PaymentProof paymentProof =
                paymentProofRepository
                        .findByIdAndCustomer_Id(
                                paymentProofId,
                                customerId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Payment receipt not found"
                                )
                        );

        return loadReceipt(paymentProof);
    }

    @Transactional(readOnly = true)
    public ReceiptDownload getAdminReceipt(
            Long adminId,
            Long paymentProofId
    ) {
        getActiveAdmin(adminId);

        PaymentProof paymentProof =
                getPaymentProof(
                        paymentProofId
                );

        return loadReceipt(paymentProof);
    }

    /*
     * =========================================================
     * NOTIFICATIONS
     * =========================================================
     */

    private void sendProofSubmittedNotifications(
            PaymentProof paymentProof
    ) {
        Order order =
                paymentProof.getOrder();

        notificationService.sendNotification(
                order.getCustomer(),
                NotificationType
                        .PAYMENT_PROOF_SUBMITTED,
                "Payment receipt submitted",
                "Your payment receipt for order "
                        + order.getOrderNumber()
                        + " has been submitted for review.",
                "PAYMENT_PROOF",
                paymentProof.getId(),
                "/customer/orders/"
                        + order.getId(),
                null
        );

        List<User> activeAdmins =
                userRepository
                        .findAllByRoleAndStatus(
                                Role.ADMIN,
                                AccountStatus.ACTIVE
                        );

        for (User admin : activeAdmins) {
            notificationService.sendNotification(
                    admin,
                    NotificationType
                            .PAYMENT_PROOF_SUBMITTED,
                    "New payment receipt",
                    order.getCustomer().getFullName()
                            + " uploaded a payment receipt for order "
                            + order.getOrderNumber()
                            + ".",
                    "PAYMENT_PROOF",
                    paymentProof.getId(),
                    "/admin/payments/"
                            + paymentProof.getId(),
                    null
            );
        }
    }

    private void sendPaymentConfirmedNotifications(
            PaymentProof paymentProof
    ) {
        Order order =
                paymentProof.getOrder();

        notificationService.sendNotification(
                order.getCustomer(),
                NotificationType.PAYMENT_CONFIRMED,
                "Payment confirmed",
                "Your payment for order "
                        + order.getOrderNumber()
                        + " has been confirmed.",
                "ORDER",
                order.getId(),
                "/customer/orders/"
                        + order.getId(),
                null
        );

        List<OrderItem> items =
                orderItemRepository
                        .findAllByOrder_IdOrderByCreatedAtAsc(
                                order.getId()
                        );

        Set<User> providers =
                items.stream()
                        .map(OrderItem::getOwner)
                        .filter(owner -> owner != null)
                        .collect(Collectors.toSet());

        for (User provider : providers) {
            notificationService.sendNotification(
                    provider,
                    NotificationType.PAYMENT_CONFIRMED,
                    "Order payment confirmed",
                    "Payment has been confirmed for order "
                            + order.getOrderNumber()
                            + ". You can begin processing your items.",
                    "ORDER",
                    order.getId(),
                    "/provider/orders",
                    null
            );
        }
    }

    /*
     * =========================================================
     * FILE HANDLING
     * =========================================================
     */

    private String saveReceiptFile(
            MultipartFile file,
            String orderNumber
    ) {
        try {
            Files.createDirectories(
                    receiptUploadDirectory
            );

            String originalFileName =
                    file.getOriginalFilename();

            String extension =
                    getFileExtension(
                            originalFileName
                    );

            String fileName =
                    orderNumber
                            + "-"
                            + UUID.randomUUID()
                            + extension;

            Path targetPath =
                    receiptUploadDirectory
                            .resolve(fileName)
                            .normalize();

            if (
                    !targetPath.startsWith(
                            receiptUploadDirectory
                    )
            ) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid receipt file path"
                );
            }

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption
                            .REPLACE_EXISTING
            );

            return targetPath.toString();

        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to save payment receipt",
                    exception
            );
        }
    }

    private ReceiptDownload loadReceipt(
            PaymentProof paymentProof
    ) {
        try {
            Path filePath =
                    Paths.get(
                            paymentProof.getReceiptUrl()
                    )
                            .toAbsolutePath()
                            .normalize();

            Resource resource =
                    new UrlResource(
                            filePath.toUri()
                    );

            if (
                    !resource.exists() ||
                    !resource.isReadable()
            ) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment receipt file was not found"
                );
            }

            return new ReceiptDownload(
                    resource,
                    paymentProof.getFileContentType(),
                    paymentProof.getOriginalFileName()
            );

        } catch (MalformedURLException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to load payment receipt",
                    exception
            );
        }
    }

    private String getFileExtension(
            String fileName
    ) {
        if (
                fileName == null ||
                !fileName.contains(".")
        ) {
            return "";
        }

        String extension =
                fileName.substring(
                        fileName.lastIndexOf(".")
                )
                        .toLowerCase();

        return switch (extension) {
            case ".jpg",
                 ".jpeg",
                 ".png",
                 ".webp",
                 ".pdf" -> extension;

            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported receipt file type"
            );
        };
    }

    /*
     * =========================================================
     * VALIDATION
     * =========================================================
     */

    private void validatePaymentProof(
            Order order,
            BigDecimal amount,
            String senderBankName,
            String senderAccountName,
            String transactionReference,
            LocalDateTime paymentDate,
            MultipartFile receiptFile
    ) {
        if (
                order.getPaymentMethod()
                        != PaymentMethod.BANK_TRANSFER
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This order does not use bank-transfer payment"
            );
        }

        if (
                order.getPaymentStatus()
                        == PaymentStatus.PAID
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order has already been paid"
            );
        }

        if (
                amount == null ||
                amount.compareTo(
                        BigDecimal.ZERO
                ) <= 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment amount must be greater than zero"
            );
        }

        if (
                order.getTotalAmount() != null &&
                amount.compareTo(
                        order.getTotalAmount()
                ) != 0
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The payment amount must match the order total of "
                            + order.getTotalAmount()
            );
        }

        requireText(
                senderBankName,
                "Sender bank name is required"
        );

        requireText(
                senderAccountName,
                "Sender account name is required"
        );

        requireText(
                transactionReference,
                "Transaction reference is required"
        );

        requireLetters(
                senderBankName,
                "Sender bank name can contain letters, spaces, apostrophes, hyphens and & only",
                true
        );

        requireLetters(
                senderAccountName,
                "Sender account name can contain letters, spaces, apostrophes and hyphens only",
                false
        );

        if (
                !transactionReference.trim()
                        .matches("^[A-Za-z0-9][A-Za-z0-9\\-_/]{3,79}$")
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter a valid transaction reference"
            );
        }

        if (paymentDate == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment date is required"
            );
        }

        if (
                paymentDate.isAfter(
                        LocalDateTime.now()
                                .plusMinutes(5)
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment date cannot be in the future"
            );
        }

        validateReceiptFile(receiptFile);
    }

    private void validateReceiptFile(
            MultipartFile file
    ) {
        if (
                file == null ||
                file.isEmpty()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment receipt file is required"
            );
        }

        if (
                file.getSize()
                        > MAX_RECEIPT_SIZE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment receipt cannot exceed 5 MB"
            );
        }

        String contentType =
                file.getContentType();

        if (
                contentType == null ||
                !ALLOWED_CONTENT_TYPES
                        .contains(contentType)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Receipt must be a JPG, PNG, WEBP or PDF file"
            );
        }
    }

    private void validateBankAccountRequest(
            BankAccountRequest request
    ) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bank account request is required"
            );
        }

        requireText(
                request.bankName(),
                "Bank name is required"
        );

        requireText(
                request.accountName(),
                "Account name is required"
        );

        requireText(
                request.accountNumber(),
                "Account number is required"
        );
    }

    private User getActiveCustomer(
            Long customerId
    ) {
        User customer =
                userRepository
                        .findById(customerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Customer account not found"
                                )
                        );

        if (
                customer.getRole()
                        != Role.CUSTOMER
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only customers can perform this action"
            );
        }

        if (
                customer.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your customer account is not active"
            );
        }

        return customer;
    }

    private User getActiveAdmin(
            Long adminId
    ) {
        User admin =
                userRepository
                        .findById(adminId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Administrator account not found"
                                )
                        );

        if (admin.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only administrators can manage payments"
            );
        }

        if (
                admin.getStatus()
                        != AccountStatus.ACTIVE
        ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "The administrator account is not active"
            );
        }

        return admin;
    }

    private PaymentProof getPaymentProof(
            Long paymentProofId
    ) {
        return paymentProofRepository
                .findById(paymentProofId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Payment receipt not found"
                        )
                );
    }

    private void requireText(
            String value,
            String message
    ) {
        if (
                value == null ||
                value.isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }
    }

    private void requireLetters(
            String value,
            String message,
            boolean allowAmpersand
    ) {
        String pattern =
                allowAmpersand
                        ? "^[A-Za-z][A-Za-z .&'\\-]{1,149}$"
                        : "^[A-Za-z][A-Za-z .'\\-]{1,199}$";

        if (
                value == null
                        || !value.trim()
                        .matches(pattern)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    message
            );
        }
    }

    private String clean(
            String value
    ) {
        if (
                value == null ||
                value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    public record ReceiptDownload(
            Resource resource,
            String contentType,
            String fileName
    ) {
    }
}