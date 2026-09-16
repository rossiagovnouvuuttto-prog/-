// Clicking the toolbar icon opens the chat in a full tab. If it is already
// open somewhere, focus that tab instead of piling up duplicates.
const PAGE = chrome.runtime.getURL('index.html');

chrome.action.onClicked.addListener(async () => {
  const [existing] = await chrome.tabs.query({ url: PAGE });
  if (existing) {
    await chrome.tabs.update(existing.id, { active: true });
    await chrome.windows.update(existing.windowId, { focused: true });
    return;
  }
  await chrome.tabs.create({ url: PAGE });
});

// Open the chat right after install, so the first run needs no hunting.
chrome.runtime.onInstalled.addListener(({ reason }) => {
  if (reason === 'install') chrome.tabs.create({ url: PAGE });
});
