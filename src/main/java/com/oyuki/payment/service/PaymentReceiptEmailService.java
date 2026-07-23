package com.oyuki.payment.service;

import com.oyuki.payment.entity.PaymentProof;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class PaymentReceiptEmailService {

    private final JavaMailSender mailSender;
    private final String adminEmail;

    public PaymentReceiptEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,

            @Value("${app.admin.notification-email:}")
            String adminEmail
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.adminEmail =
                adminEmail == null
                        ? ""
                        : adminEmail.trim();
    }

    /*
     * Email delivery is optional. If SMTP or the admin email
     * is not configured, the receipt remains available in the
     * admin dashboard and checkout is not interrupted.
     */
    public void sendReceiptSubmitted(
            PaymentProof paymentProof
    ) {
        if (mailSender == null || adminEmail.isBlank()) {
            return;
        }

        try {
            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );

            helper.setTo(adminEmail);
            helper.setSubject(
                    "Oyuki payment receipt - "
                            + paymentProof
                            .getOrder()
                            .getOrderNumber()
            );

            helper.setText(
                    "A customer uploaded a payment receipt.\n\n"
                            + "Order: "
                            + paymentProof
                            .getOrder()
                            .getOrderNumber()
                            + "\nCustomer: "
                            + paymentProof
                            .getCustomer()
                            .getFullName()
                            + "\nAmount: ₦"
                            + paymentProof.getAmount()
                            + "\nBank: "
                            + paymentProof
                            .getSenderBankName()
                            + "\nReference: "
                            + paymentProof
                            .getTransactionReference()
                            + "\n\nOpen the Oyuki admin dashboard to confirm or reject the payment."
            );

            Path receiptPath =
                    Path.of(
                            paymentProof
                                    .getReceiptUrl()
                    );

            if (
                    receiptPath.toFile()
                            .exists()
            ) {
                helper.addAttachment(
                        paymentProof
                                .getOriginalFileName() == null
                                ? "payment-receipt"
                                : paymentProof
                                .getOriginalFileName(),
                        new FileSystemResource(
                                receiptPath
                        )
                );
            }

            mailSender.send(message);

        } catch (Exception exception) {
            System.err.println(
                    "Payment receipt email was not sent: "
                            + exception.getMessage()
            );
        }
    }
}
