'use strict';
async function loadNotifications() {
  const container = document.getElementById('notificationList');
  container.textContent = '正在加载...';
  try {
    const response = await fetch('/api/notifications?limit=50', {credentials:'same-origin'});
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const records = await response.json();
    container.replaceChildren();
    if (!records.length) {
      container.textContent = '暂无通知';
      return;
    }
    for (const item of records) {
      const row = document.createElement('article');
      row.className = 'card';
      const text = document.createElement('div');
      text.textContent = item.message || 'Notification';
      const meta = document.createElement('small');
      meta.textContent = `Ticket #${item.ticketId} · ${item.createdAt || ''}`;
      row.append(text,meta);
      container.append(row);
    }
  } catch (error) {
    container.textContent = `无法加载通知：${error.message}`;
  }
}
document.getElementById('refresh').addEventListener('click', loadNotifications);
loadNotifications();
