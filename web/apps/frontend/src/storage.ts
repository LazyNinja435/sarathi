const DEMO_CLIENT_STORAGE = "sarathi.demoClientId";
const GUEST_SESSION_STORAGE = "sarathi.guestSession";
const SIGN_IN_BANNER_DISMISSED_STORAGE = "sarathi.signInPersonalizationBannerDismissed";

export function getDemoClientId() {
  const existing = localStorage.getItem(DEMO_CLIENT_STORAGE);
  if (existing) return existing;
  const next = crypto.randomUUID();
  localStorage.setItem(DEMO_CLIENT_STORAGE, next);
  return next;
}

export function readGuestSession() {
  return localStorage.getItem(GUEST_SESSION_STORAGE) === "true";
}

export function saveGuestSession(enabled: boolean) {
  if (enabled) localStorage.setItem(GUEST_SESSION_STORAGE, "true");
  else localStorage.removeItem(GUEST_SESSION_STORAGE);
}

export function readSignInPersonalizationBannerDismissed() {
  return localStorage.getItem(SIGN_IN_BANNER_DISMISSED_STORAGE) === "true";
}

export function dismissSignInPersonalizationBanner() {
  localStorage.setItem(SIGN_IN_BANNER_DISMISSED_STORAGE, "true");
}
