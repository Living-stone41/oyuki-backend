(function () {
  "use strict";

  const O = window.Oyuki;

  if (!O) {
    console.error(
      "Oyuki is unavailable. Make sure app.js loads before referrals.js."
    );
    return;
  }

  const q = (selector) =>
    document.querySelector(selector);

  const formatMoney = (amount) => {
    return new Intl.NumberFormat("en-NG", {
      style: "currency",
      currency: "NGN",
      minimumFractionDigits: 2
    }).format(Number(amount || 0));
  };

  let referralData = {
    code: "",
    totalInvited: 0,
    verifiedInvited: 0,
    totalEarned: 0,
    history: []
  };

  function getToken() {
    if (
      O.Auth &&
      typeof O.Auth.token === "function"
    ) {
      return O.Auth.token();
    }

    return localStorage.getItem("oyuki_token");
  }

  function getCurrentUser() {
    if (
      O.Auth &&
      typeof O.Auth.current === "function"
    ) {
      return O.Auth.current() || {};
    }

    try {
      return JSON.parse(
        localStorage.getItem("oyuki_user") || "{}"
      );
    } catch (error) {
      return {};
    }
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

    const element =
      document.createElement("div");

    element.textContent =
      String(value || "");

    return element.innerHTML;
  }

  function formatDate(value) {
    if (!value) {
      return "Recently";
    }

    if (typeof O.date === "function") {
      return O.date(value);
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return "Recently";
    }

    return date.toLocaleDateString("en-NG", {
      day: "numeric",
      month: "short",
      year: "numeric"
    });
  }

  function showToast(
    message,
    type = "success"
  ) {
    if (
      O.Toast &&
      typeof O.Toast.show === "function"
    ) {
      O.Toast.show(message, type);
      return;
    }

    console.log(message);
  }

  function generateFallbackCode() {
    const user = getCurrentUser();

    const source =
      user.fullName ||
      user.name ||
      user.email ||
      "OYUKI";

    const prefix = source
      .replace(/[^a-z0-9]/gi, "")
      .slice(0, 8)
      .toUpperCase();

    const userId =
      user.id ||
      Math.floor(
        Math.random() * 9000 + 1000
      );

    const suffix = String(userId)
      .slice(-4)
      .padStart(4, "0");

    return `${prefix || "OYUKI"}-${suffix}`;
  }

  function loadCachedReferral() {
    try {
      const cachedCode =
        localStorage.getItem(
          "oyuki_referral_code"
        );

      const cachedData = JSON.parse(
        localStorage.getItem(
          "oyuki_referral_preview"
        ) || "null"
      );

      if (cachedData) {
        referralData = {
          ...referralData,
          ...cachedData,
          history:
            Array.isArray(cachedData.history)
              ? cachedData.history
              : []
        };
      }

      if (cachedCode) {
        referralData.code = cachedCode;
      }
    } catch (error) {
      console.warn(
        "Could not load cached referral information:",
        error
      );
    }
  }

  function saveReferralPreview() {
    try {
      localStorage.setItem(
        "oyuki_referral_code",
        referralData.code
      );

      localStorage.setItem(
        "oyuki_referral_preview",
        JSON.stringify(referralData)
      );
    } catch (error) {
      console.warn(
        "Could not save referral information:",
        error
      );
    }
  }

  async function loadReferralData() {
    try {
      if (
        !O.Api ||
        typeof O.Api.get !== "function"
      ) {
        throw new Error(
          "Referral API client is unavailable."
        );
      }

      const response =
        await O.Api.get("/referrals/me");

      const result =
        unwrapResponse(response) || {};

      referralData = {
        ...referralData,
        ...result,
        code:
          result.code ||
          result.referralCode ||
          referralData.code,
        totalInvited:
          result.totalInvited ??
          result.referralCount ??
          referralData.totalInvited,
        verifiedInvited:
          result.verifiedInvited ??
          result.qualifiedCount ??
          referralData.verifiedInvited,
        totalEarned:
          result.totalEarned ??
          result.referralEarnings ??
          referralData.totalEarned,
        history:
          Array.isArray(result.history)
            ? result.history
            : Array.isArray(result.referrals)
              ? result.referrals
              : []
      };

      if (!referralData.code) {
        referralData.code =
          generateFallbackCode();
      }

      saveReferralPreview();
    } catch (error) {
      console.warn(
        "Referral API is not ready:",
        error.message
      );

      loadCachedReferral();

      if (!referralData.code) {
        referralData.code =
          generateFallbackCode();
      }

      saveReferralPreview();
    }

    renderReferralPage();
  }

  function renderReferralPage() {
    const referralCode =
      q("#referralCode");

    const totalInvited =
      q("#totalInvited");

    const verifiedInvited =
      q("#verifiedInvited");

    const totalEarned =
      q("#totalEarned");

    const code =
      referralData.code ||
      referralData.referralCode ||
      generateFallbackCode();

    if (referralCode) {
      referralCode.textContent = code;
    }

    if (totalInvited) {
      totalInvited.textContent =
        referralData.totalInvited ??
        referralData.referralCount ??
        0;
    }

    if (verifiedInvited) {
      verifiedInvited.textContent =
        referralData.verifiedInvited ??
        referralData.qualifiedCount ??
        0;
    }

    if (totalEarned) {
      totalEarned.textContent =
        formatMoney(
          referralData.totalEarned ??
          referralData.referralEarnings ??
          0
        );
    }

    const qualified = Number(referralData.qualifiedReferralCount ?? referralData.verifiedInvited ?? 0);
    const minimum = Number(referralData.minimumWithdrawalReferrals || 20);
    const rewardText = q("#referralRewardText");
    const progressCount = q("#referralProgressCount");
    const progressBar = q("#referralProgressBar");
    const eligibilityText = q("#referralEligibilityText");
    if (rewardText) rewardText.textContent = referralData.marketer
      ? `Earn ${formatMoney(referralData.rewardPerVerifiedReferral || 2000)} for each verified Seller or Kitchen.`
      : `Earn ${formatMoney(referralData.rewardPerVerifiedReferral || 200)} for each verified registration.`;
    if (progressCount) progressCount.textContent = `${qualified} / ${minimum}`;
    if (progressBar) progressBar.style.width = `${Math.min(100, (qualified / minimum) * 100)}%`;
    if (eligibilityText) eligibilityText.textContent = referralData.withdrawalEligible
      ? "You have reached the withdrawal requirement."
      : `You need ${Math.max(0, minimum - qualified)} more qualified referrals before withdrawal.`;

    renderReferralHistory(
      referralData.history ||
      referralData.referrals ||
      []
    );
  }

  function renderReferralHistory(items) {
    const container =
      q("#referralHistory");

    if (!container) {
      return;
    }

    if (
      !Array.isArray(items) ||
      items.length === 0
    ) {
      container.innerHTML = `
        <div class="wallet-empty">
          <i class="bi bi-people"></i>

          <h4>No referrals yet</h4>

          <p>
            Share your referral link to start earning.
          </p>
        </div>
      `;

      return;
    }

    container.innerHTML = items
      .map((referral) => {
        const name =
          referral.referredName ||
          referral.name ||
          referral.referredUserName ||
          "New Oyuki member";

        const status =
          referral.status ||
          "PENDING";

        const reward =
          referral.reward ??
          referral.referrerReward ??
          referral.amount ??
          0;

        const isRewarded =
          String(status).toUpperCase() ===
          "REWARDED";

        return `
          <article class="wallet-tx">

            <div
              class="wallet-tx-icon ${
                isRewarded
                  ? "credit"
                  : ""
              }"
            >
              <i
                class="bi ${
                  isRewarded
                    ? "bi-person-check"
                    : "bi-person-plus"
                }"
              ></i>
            </div>

            <div class="wallet-tx-copy">

              <strong>
                ${escapeHtml(name)}
              </strong>

              <span>
                ${escapeHtml(status)}
                ·
                ${escapeHtml(
                  formatDate(
                    referral.createdAt
                  )
                )}
              </span>

            </div>

            <b
              class="${
                isRewarded
                  ? "credit-text"
                  : ""
              }"
            >
              ${formatMoney(reward)}
            </b>

          </article>
        `;
      })
      .join("");
  }

  function getReferralLink() {
    const codeElement =
      q("#referralCode");

    const code =
      codeElement?.textContent?.trim() ||
      referralData.code ||
      generateFallbackCode();

    return (
      `${window.location.origin}` +
      `/register.html?ref=` +
      encodeURIComponent(code)
    );
  }

  async function copyReferralLink() {
    const referralLink =
      getReferralLink();

    try {
      if (
        navigator.clipboard &&
        window.isSecureContext
      ) {
        await navigator.clipboard.writeText(
          referralLink
        );
      } else {
        const temporaryInput =
          document.createElement("textarea");

        temporaryInput.value =
          referralLink;

        temporaryInput.style.position =
          "fixed";

        temporaryInput.style.opacity =
          "0";

        document.body.appendChild(
          temporaryInput
        );

        temporaryInput.focus();
        temporaryInput.select();

        document.execCommand("copy");

        temporaryInput.remove();
      }

      showToast(
        "Referral link copied.",
        "success"
      );
    } catch (error) {
      console.error(
        "Could not copy referral link:",
        error
      );

      showToast(
        "Unable to copy the referral link.",
        "error"
      );
    }
  }

  async function shareReferralLink() {
    const referralLink =
      getReferralLink();

    const shareData = {
      title: "Join Oyuki",
      text:
        "Register on Oyuki using my referral link and receive a welcome reward after OTP verification.",
      url: referralLink
    };

    try {
      if (
        navigator.share &&
        navigator.canShare?.(shareData)
      ) {
        await navigator.share(shareData);
        return;
      }

      await copyReferralLink();
    } catch (error) {
      if (
        error.name !== "AbortError"
      ) {
        console.error(
          "Could not share referral link:",
          error
        );

        await copyReferralLink();
      }
    }
  }

  document.addEventListener(
    "DOMContentLoaded",
    () => {
      const token = getToken();

      if (!token) {
        window.location.href =
          "login.html?next=referrals.html";
        return;
      }

      const copyButton =
        q("#copyReferral");

      const shareButton =
        q("#shareReferral");

      if (copyButton) {
        copyButton.addEventListener(
          "click",
          copyReferralLink
        );
      }

      if (shareButton) {
        shareButton.addEventListener(
          "click",
          shareReferralLink
        );
      }

      loadReferralData();
    }
  );
})();