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
