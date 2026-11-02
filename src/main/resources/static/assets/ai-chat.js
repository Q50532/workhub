'use strict';
const form = document.getElementById('chatForm');
const field = document.getElementById('question');
const send = document.getElementById('sendButton');
const messages = document.getElementById('messages');
const statusLine = document.getElementById('status');
let csrf = null;
let sending = false;

function displayMessage(text, role, citations = [], tools = []) {
  const item = document.createElement('article');
  item.className = 'message ' + role;
  item.textContent = text; // Never inject LLM content as HTML.
  if (role === 'assistant' && (citations.length || tools.length)) {
    const meta = document.createElement('div');
    meta.className = 'meta';
    const parts = [];
    if (tools.length) parts.push('Tools: ' + tools.join(', '));
    if (citations.length) parts.push('Sources: ' + citations.join(', '));
    meta.textContent = parts.join('  ·  ');
    item.append(meta);
  }
  messages.append(item);
  messages.scrollTop = messages.scrollHeight;
}

async function initialize() {
  try {
    const me = await fetch('/api/me', {credentials:'same-origin'});
    if (me.status === 401 || me.redirected) { window.location.href = '/login.html'; return; }
    if (!me.ok) throw new Error('无法读取登录信息');
    const user = await me.json();
    document.getElementById('identity').textContent = user.username || '';
    const response = await fetch('/csrf', {credentials:'same-origin'});
    if (!response.ok) throw new Error('无法获取 CSRF Token');
    const token = await response.json();
    csrf = {headerName: token.headerName, token: token.token};
  } catch (_) {
    statusLine.textContent = '登录会话或 CSRF 校验不可用，请重新登录后刷新页面。';
    statusLine.classList.add('error');
  }
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (sending || !csrf) return;
  const question = field.value.trim();
  if (!question || question.length > 2000) return;
  sending = true;
  send.disabled = true;
  field.disabled = true;
  displayMessage(question, 'user');
  field.value = '';
  statusLine.classList.remove('error');
  statusLine.textContent = '正在查询 Agent…';
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 60000);
    let response;
    try {
      response = await fetch('/api/ai/chat', {
        method:'POST', credentials:'same-origin', signal:controller.signal,
        headers:{'Content-Type':'application/json', [csrf.headerName]:csrf.token},
        body:JSON.stringify({message:question})
      });
    } finally { clearTimeout(timeoutId); }
    if (response.status === 401) throw new Error('登录已过期，请重新登录');
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(body.error || `Agent error: HTTP ${response.status}`);
    displayMessage(body.answer || '没有返回回答', 'assistant', body.citations || [], body.tools || []);
    statusLine.textContent = '';
  } catch (error) {
    const message = error.name === 'AbortError' ? 'Agent 响应超时，请稍后重试' : error.message;
    statusLine.textContent = message;
    statusLine.classList.add('error');
    displayMessage('本次请求失败：' + message, 'assistant');
  } finally {
    sending = false;
    send.disabled = false;
    field.disabled = false;
    field.focus();
  }
});
initialize();
