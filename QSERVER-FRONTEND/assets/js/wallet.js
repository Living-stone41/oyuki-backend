(function () {
  "use strict";

  const O = window.Oyuki;

  if (!O) {
    console.error(
      "Oyuki is not available. Make sure app.js loads before wallet.js."
    );
    return;
  }

  const q = (selector) => document.querySelector(selector);

  const formatMoney = (amount) => {
    return new Intl.NumberFormat("en-NG", {
      style: "currency",
      currency: "NGN",
      minimumFractionDigits: 2
    }).format(Number(amount || 0));
  };

  let state = {
    availableBalance: 0,
    pendingBalance: 0,
    referralEarnings: 0,
    transactions: [],
    qualifiedReferralCount: 0,
    minimumWithdrawalReferrals: 20,
    withdrawalEligible: false
  };

  function getToken() {
    if (O.Auth && typeof O.Auth.token === "function") {
      return O.Auth.token();
    }

    return localStorage.getItem("oyuki_token");
  }

  function unwrapResponse(response) {
    if (typeof O.unwrap === "function") {
      return O.unwrap(response);
    }

    return response?.data ?? response;
  }

  function escapeHtml(value) {
    if (typeof O.escapeHtml === "function") {
      return O.escapeHtml(String(value || ""));
    }

    const element = document.createElement("div");
    element.textContent = String(value || "");
    return element.innerHTML;
  }

  function formatDate(value) {
    if (!value) {
      return "Recently";
    }

    if (typeof O.date === "function") {
      return O.date(value);
    }

    const parsedDate = new Date(value);

    if (Number.isNaN(parsedDate.getTime())) {
      return "Recently";
    }

    return parsedDate.toLocaleDateString("en-NG", {
      day: "numeric",
      month: "short",
      year: "numeric"
    });
  }

  function showToast(message, type = "success") {
    if (
      O.Toast &&
      typeof O.Toast.show === "function"
    ) {
      O.Toast.show(message, type);
      return;
    }

    console.log(message);
  }

  async function getWallet() {
    try {
      if (!O.Api || typeof O.Api.get !== "function") {
        throw new Error("Wallet API client is unavailable.");
      }

      const response = await O.Api.get("/wallet");
      const result = unwrapResponse(response) || {};

      state = {
        ...state,
        ...result,
        availableBalance:
          result.availableBalance ??
          result.balance ??
          state.availableBalance,
        pendingBalance:
          result.pendingBalance ??
          state.pendingBalance,
        referralEarnings:
          result.referralEarnings ??
          state.referralEarnings,
        transactions:
          Array.isArray(result.transactions)
            ? result.transactions
            : []
      };

      savePreview();
    } catch (error) {
      console.warn(
        "Wallet API is not ready:",
        error.message
      );

      loadPreview();
    }

    renderWallet();
  }

  function savePreview() {
    try {
      localStorage.setItem(
        "oyuki_wallet_preview",
        JSON.stringify(state)
      );
    } catch (error) {
      console.warn(
        "Unable to save wallet preview:",
        error
      );
    }
  }

  function loadPreview() {
    try {
      const cached = JSON.parse(
        localStorage.getItem(
          "oyuki_wallet_preview"
        ) || "null"
      );

      if (cached) {
        state = {
          ...state,
          ...cached,
          transactions:
            Array.isArray(cached.transactions)
              ? cached.transactions
              : []
        };
      }
    } catch (error) {
      console.warn(
        "Unable to load wallet preview:",
        error
      );
    }
  }

  function renderWallet() {
    const walletBalance = q("#walletBalance");
    const pendingBalance = q("#pendingBalance");
    const referralEarnings = q(
      "#referralEarnings"
    );

    if (walletBalance) {
      walletBalance.textContent = formatMoney(
        state.availableBalance ??
        state.balance
      );
    }

    if (pendingBalance) {
      pendingBalance.textContent = formatMoney(
        state.pendingBalance
      );
    }

    if (referralEarnings) {
      referralEarnings.textContent = formatMoney(
        state.referralEarnings
      );
    }

    const qualified = Number(state.qualifiedReferralCount || 0);
    const minimum = Number(state.minimumWithdrawalReferrals || 20);
    const progress = q("#walletReferralProgress");
    const progressBar = q("#walletProgressBar");
    const eligibilityText = q("#walletEligibilityText");
    const withdrawButton = q("#walletWithdrawButton");
    if (progress) progress.textContent = `${qualified} / ${minimum}`;
    if (progressBar) progressBar.style.width = `${Math.min(100, (qualified / minimum) * 100)}%`;
    if (eligibilityText) eligibilityText.textContent = state.withdrawalEligible
      ? "You can request a withdrawal."
      : `You need ${Math.max(0, minimum - qualified)} more qualified referrals before withdrawing.`;
    if (withdrawButton) {
      withdrawButton.disabled = !state.withdrawalEligible;
      withdrawButton.title = state.withdrawalEligible ? "Withdraw funds" : eligibilityText?.textContent || "Withdrawal locked";
    }

    renderTransactions(
      state.transactions || []
    );
  }

  function renderTransactions(items) {
    const container = q(
      "#walletTransactions"
    );

    if (!container) {
      return;
    }

    if (!Array.isArray(items) || !items.length) {
      container.innerHTML = `
        <div class="wallet-empty">
          <i class="bi bi-receipt"></i>

          <h4>No transactions yet</h4>

          <p>
            Wallet funding, referral rewards and
            payments will appear here.
          </p>
        </div>
      `;

      return;
    }

    container.innerHTML = items
      .map((transaction) => {
        const transactionType = String(
          transaction.direction ||
          transaction.type ||
          ""
        ).toUpperCase();

        const numericAmount = Number(
          transaction.amount || 0
        );

        const isCredit =
          transactionType.includes("CREDIT") ||
          transactionType.includes("REWARD") ||
          transactionType.includes("FUND") ||
          numericAmount > 0;

        const description =
          transaction.description ||
          String(
            transaction.type ||
            "Wallet transaction"
          ).replaceAll("_", " ");

        const status =
          transaction.status ||
          "COMPLETED";

        return `
          <article
            class="wallet-tx"
            data-direction="${
              isCredit
                ? "CREDIT"
                : "DEBIT"
            }"
          >
            <div
              class="wallet-tx-icon ${
                isCredit
                  ? "credit"
                  : "debit"
              }"
            >
              <i
                class="bi ${
                  isCredit
                    ? "bi-arrow-down-left"
                    : "bi-arrow-up-right"
                }"
              ></i>
            </div>

            <div class="wallet-tx-copy">
              <strong>
                ${escapeHtml(description)}
              </strong>

              <span>
                ${escapeHtml(
                  formatDate(
                    transaction.createdAt
                  )
                )}
                ·
                ${escapeHtml(status)}
              </span>
            </div>

            <b
              class="${
                isCredit
                  ? "credit-text"
                  : "debit-text"
              }"
            >
              ${isCredit ? "+" : "-"}${formatMoney(
                Math.abs(numericAmount)
              )}
            </b>
          </article>
        `;
      })
      .join("");
  }

  function openModal(type) {
    if (type === "withdraw" && !state.withdrawalEligible) {
      showToast(`You need ${Math.max(0, Number(state.minimumWithdrawalReferrals || 20) - Number(state.qualifiedReferralCount || 0))} more qualified referrals before withdrawing.`, "error");
      return;
    }
    const modal = q("#walletModal");
    const modalContent = q(
      "#walletModalContent"
    );

    if (!modal || !modalContent) {
      return;
    }

    const isFunding = type === "fund";

    modalContent.innerHTML = `
      <p class="wallet-eyebrow">
        ${
          isFunding
            ? "ADD MONEY"
            : "WITHDRAW"
        }
      </p>

      <h3>
        ${
          isFunding
            ? "Fund your wallet"
            : "Withdraw funds"
        }
      </h3>

      <p>
        ${
          isFunding
            ? "Payments are securely processed with Paystack."
            : "Withdraw funds to your verified bank account."
        }
      </p>

      <form id="walletActionForm">

        <label for="walletAmount">
          Amount
        </label>

        <div class="wallet-amount-input">
          <span>₦</span>

          <input
            id="walletAmount"
            name="amount"
            type="number"
            min="100"
            step="100"
            required
            placeholder="0.00"
          >
        </div>

        ${
          isFunding
            ? ""
            : `
              <label for="accountNumber">
                Bank account number
              </label>

              <input
                id="accountNumber"
                name="accountNumber"
                type="text"
                maxlength="10"
                inputmode="numeric"
                required
                placeholder="10-digit account number"
              >

              <label for="bankCode">
                Bank code
              </label>

              <input
                id="bankCode"
                name="bankCode"
                type="text"
                required
                placeholder="Enter bank code"
              >

              <label for="accountName">
                Account name
              </label>

              <input
                id="accountName"
                name="accountName"
                type="text"
                required
                placeholder="Enter account name"
              >
            `
        }

        <button
          class="wallet-primary"
          type="submit"
        >
          ${
            isFunding
              ? "Continue with Paystack"
              : "Request withdrawal"
          }
        </button>

        <small id="walletFormMessage"></small>

      </form>
    `;

    modal.classList.add("show");
    modal.setAttribute(
      "aria-hidden",
      "false"
    );

    const form = q("#walletActionForm");

    if (form) {
      form.addEventListener(
        "submit",
        async (event) => {
          event.preventDefault();

          await submitWalletAction(
            event.currentTarget,
            isFunding
          );
        }
      );
    }
  }

  async function submitWalletAction(
    form,
    isFunding
  ) {
    const message = q(
      "#walletFormMessage"
    );

    const submitButton =
      form.querySelector(
        'button[type="submit"]'
      );

    const formData = new FormData(form);
    const payload =
      Object.fromEntries(
        formData.entries()
      );

    payload.amount = Number(
      payload.amount
    );

    if (
      !payload.amount ||
      payload.amount < 100
    ) {
      if (message) {
        message.textContent =
          "Enter an amount of at least ₦100.";
        message.className = "error";
      }

      return;
    }

    try {
      if (
        !O.Api ||
        typeof O.Api.post !== "function"
      ) {
        throw new Error(
          "Wallet API client is unavailable."
        );
      }

      if (submitButton) {
        submitButton.disabled = true;
        submitButton.textContent =
          "Please wait...";
      }

      const path = isFunding
        ? "/wallet/fund/initialize"
        : "/wallet/withdrawals";

      const response = await O.Api.post(
        path,
        payload
      );

      const result =
        unwrapResponse(response) || {};

      const authorizationUrl =
        result.authorizationUrl ||
        result.authorization_url ||
        result.data?.authorization_url;

      if (
        isFunding &&
        authorizationUrl
      ) {
        window.location.href =
          authorizationUrl;
        return;
      }

      const successMessage = isFunding
        ? "Wallet funding started."
        : "Withdrawal request submitted.";

      if (message) {
        message.textContent =
          successMessage;
        message.className = "success";
      }

      showToast(
        successMessage,
        "success"
      );

      setTimeout(() => {
        closeModal();
        getWallet();
      }, 800);
    } catch (error) {
      if (message) {
        message.textContent =
          error.message ||
          "Unable to complete this request.";
        message.className = "error";
      }

      showToast(
        error.message ||
        "Unable to complete this request.",
        "error"
      );
    } finally {
      if (submitButton) {
        submitButton.disabled = false;
        submitButton.textContent =
          isFunding
            ? "Continue with Paystack"
            : "Request withdrawal";
      }
    }
  }

  function closeModal() {
    const modal = q("#walletModal");

    if (!modal) {
      return;
    }

    modal.classList.remove("show");
    modal.setAttribute(
      "aria-hidden",
      "true"
    );
  }

  function setupBalanceToggle() {
    const toggleButton = q(
      "#toggleBalance"
    );

    const balanceElement = q(
      "#walletBalance"
    );

    if (
      !toggleButton ||
      !balanceElement
    ) {
      return;
    }

    let hidden = false;

    toggleButton.addEventListener(
      "click",
      () => {
        hidden = !hidden;

        balanceElement.textContent =
          hidden
            ? "₦••••••"
            : formatMoney(
                state.availableBalance ??
                state.balance
              );

        const icon =
          toggleButton.querySelector("i");

        if (icon) {
          icon.className = hidden
            ? "bi bi-eye-slash"
            : "bi bi-eye";
        }

        toggleButton.setAttribute(
          "aria-label",
          hidden
            ? "Show wallet balance"
            : "Hide wallet balance"
        );
      }
    );
  }

  function setupTransactionFilter() {
    const filter = q(
      "#transactionFilter"
    );

    if (!filter) {
      return;
    }

    filter.addEventListener(
      "change",
      (event) => {
        const selectedValue =
          event.target.value;

        document
          .querySelectorAll(
            ".wallet-tx"
          )
          .forEach((transaction) => {
            transaction.hidden =
              selectedValue !== "ALL" &&
              transaction.dataset.direction !==
                selectedValue;
          });
      }
    );
  }

  function setupWalletActions() {
    document
      .querySelectorAll(
        "[data-wallet-action]"
      )
      .forEach((button) => {
        button.addEventListener(
          "click",
          () => {
            openModal(
              button.dataset.walletAction
            );
          }
        );
      });
  }

  document.addEventListener(
    "DOMContentLoaded",
    () => {
      const token = getToken();

      if (!token) {
        window.location.href =
          "login.html?next=wallet.html";
        return;
      }

      setupWalletActions();
      setupBalanceToggle();
      setupTransactionFilter();

      const closeButton = q(
        "#walletModalClose"
      );

      const modal = q(
        "#walletModal"
      );

      if (closeButton) {
        closeButton.addEventListener(
          "click",
          closeModal
        );
      }

      if (modal) {
        modal.addEventListener(
          "click",
          (event) => {
            if (
              event.target === modal
            ) {
              closeModal();
            }
          }
        );
      }

      document.addEventListener(
        "keydown",
        (event) => {
          if (event.key === "Escape") {
            closeModal();
          }
        }
      );

      getWallet();
    }
  );
})();