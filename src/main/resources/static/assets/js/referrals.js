(function () {
  "use strict";

  const O = window.Oyuki;
  if (!O) {
    console.error("Oyuki is unavailable. Load app.js before referrals.js.");
    return;
  }

  const q = (selector) => document.querySelector(selector);
  const money = (amount) => new Intl.NumberFormat("en-NG", {
    style: "currency", currency: "NGN", minimumFractionDigits: 2
  }).format(Number(amount || 0));
  const escape = (value) => O.escapeHtml ? O.escapeHtml(String(value || "")) : String(value || "");
  const date = (value) => value ? (O.date ? O.date(value) : new Date(value).toLocaleDateString("en-NG")) : "Recently";

  let data = {
    code: "", totalInvited: 0, verifiedInvited: 0, qualifiedReferrals: 0,
    minimumWithdrawalReferrals: 20, remainingForWithdrawal: 20,
    withdrawalEligible: false, totalEarned: 0, rewardPerVerifiedReferral: 200,
    referrerType: "NORMAL", history: []
  };

  function token() {
    return O.Auth?.token?.() || localStorage.getItem("oyuki_token");
  }

  async function load() {
    try {
      const response = await O.Api.get("/referrals/me");
      const result = O.unwrap ? O.unwrap(response) : (response?.data ?? response);
      data = { ...data, ...(result || {}) };
    } catch (error) {
      console.error("Unable to load referral information:", error);
      O.Toast?.show?.(error.message || "Unable to load referral information.", "error");
    }
    render();
  }

<<<<<<< HEAD
  function render() {
    const marketer = String(data.referrerType || data.role || "").toUpperCase() === "MARKETER";
    const qualified = Number(data.qualifiedReferrals || 0);
    const minimum = Number(data.minimumWithdrawalReferrals || 20);
    const percent = Math.min(100, Math.round((qualified / Math.max(1, minimum)) * 100));

    q("#referralCode").textContent = data.code || data.referralCode || "Not available";
    q("#totalInvited").textContent = data.totalInvited || 0;
    q("#verifiedInvited").textContent = data.verifiedInvited || 0;
    q("#totalEarned").textContent = money(data.totalEarned || 0);
    q("#qualifiedReferrals").textContent = qualified;
    q("#minimumReferrals").textContent = minimum;
    q("#referralProgressBar").style.width = `${percent}%`;
    q("#referrerTypeLabel").textContent = marketer ? "Oyuki marketer" : "Oyuki referral member";

    const reward = money(data.rewardPerVerifiedReferral || (marketer ? 2000 : 200));
    q("#referralRewardText").textContent = marketer
      ? `Earn ${reward} for every OTP-verified Seller/Farmer or Kitchen registration.`
      : `Earn ${reward} for every OTP-verified registration.`;

    const remaining = Math.max(0, minimum - qualified);
    q("#withdrawalProgressText").textContent = data.withdrawalEligible
      ? "You have reached the referral withdrawal requirement."
      : marketer
        ? `You need ${remaining} more verified Seller/Farmer or Kitchen sign-up${remaining === 1 ? "" : "s"} before withdrawing.`
        : `You need ${remaining} more verified sign-up${remaining === 1 ? "" : "s"} before withdrawing referral earnings.`;

    const button = q("#withdrawReferralButton");
    button.classList.toggle("disabled", !data.withdrawalEligible);
    button.setAttribute("aria-disabled", String(!data.withdrawalEligible));
    button.onclick = data.withdrawalEligible ? null : (event) => {
      event.preventDefault();
      O.Toast?.show?.(q("#withdrawalProgressText").textContent, "error");
=======
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
>>>>>>> 1f72347 (Update Oyuki backend)
    };

    renderHistory(data.history || data.referrals || []);
  }

  function renderHistory(items) {
    const box = q("#referralHistory");
    if (!items.length) {
      box.innerHTML = '<div class="wallet-empty"><i class="bi bi-people"></i><h4>No referrals yet</h4><p>Share your referral link to start earning.</p></div>';
      return;
    }
    box.innerHTML = items.map((r) => `
      <article class="wallet-tx">
        <div class="wallet-tx-icon ${String(r.status).toUpperCase() === "REWARDED" ? "credit" : ""}">
          <i class="bi bi-person-check"></i>
        </div>
        <div class="wallet-tx-copy">
          <strong>${escape(r.referredName || "New Oyuki member")}</strong>
          <span>${escape(r.referredRole || "USER")} · ${escape(r.status || "PENDING")} · ${escape(date(r.createdAt))}</span>
        </div>
        <b class="credit-text">${money(r.reward || r.referrerReward || 0)}</b>
      </article>`).join("");
  }

  function referralLink() {
    return `${location.origin}/register.html?ref=${encodeURIComponent(q("#referralCode").textContent.trim())}`;
  }

  async function copy() {
    await navigator.clipboard.writeText(referralLink());
    O.Toast?.show?.("Referral link copied.", "success");
  }

  document.addEventListener("DOMContentLoaded", () => {
    if (!token()) {
      location.href = "login.html?next=referrals.html";
      return;
    }
    q("#copyReferral").onclick = copy;
    q("#shareReferral").onclick = async () => {
      const payload = { title: "Join Oyuki", text: "Register on Oyuki with my referral link.", url: referralLink() };
      if (navigator.share) await navigator.share(payload); else await copy();
    };
    load();
  });
})();
