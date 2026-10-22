'use strict';

// WorkHub frontend: plain JavaScript against the existing Spring Boot REST endpoints.
// The browser authenticates with Spring Security's HttpOnly session cookie.
const state = {
  user: null,
  view: 'overview',
  page: 1,
  filters: {status: '', priority: '', assignedTo: '', sortBy: 'createdAt', order: 'desc', size: 10},
  result: null,
  activeTicket: null,
  comments: [],
  logs: [],
  commentsError: '',
  logsError: '',
  detailTab: 'comments',
  editing: false,
  busy: false,
  listRequest: 0
};

const $ = id => document.getElementById(id);
const esc = value => String(value ?? '').replace(/[&<>"']/g, char => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[char]));
const icon = name => `<svg class="icon" aria-hidden="true"><use href="#i-${name}"/></svg>`;
const statusText = {OPEN: 'Open', IN_PROGRESS: 'In Progress', RESOLVED: 'Resolved', CLOSED: 'Closed'};
const priorityText = {LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High', URGENT: 'Urgent'};

const categoryChoices = [
  {value: 'BACKEND', label: 'Backend（后端）'},
  {value: 'DATABASE', label: 'Database（数据库）'},
  {value: 'FRONTEND', label: 'Frontend（前端）'},
  {value: 'OTHER', label: 'Other（其他）'}
];
function categoryOptionsHtml(current) {
  // Preserve any existing category not listed in the new dropdown.
  const choices = categoryChoices.some(item => item.value === current) || !current
    ? categoryChoices
    : [{value: current, label: current}, ...categoryChoices];
  return choices.map(item => `<option value="${esc(item.value)}"${item.value === current ? ' selected' : ''}>${esc(item.label)}</option>`).join('');
}

const nextStatus = {OPEN: 'IN_PROGRESS', IN_PROGRESS: 'RESOLVED', RESOLVED: 'CLOSED'};

function toast(message, isError = false) {
  const item = document.createElement('div');
  item.className = `toast${isError ? ' toast-error' : ''}`;
  item.textContent = message;
  $('toastStack').appendChild(item);
  setTimeout(() => item.remove(), 4200);
}

function localDate(value) {
  // MySQL LocalDateTime has no timezone. Display its stored wall-clock value without converting it.
  return value ? esc(String(value).replace('T', ' ').slice(0, 16)) : '—';
}

function statusBadge(value) {
  const safe = statusText[value] ? value : 'UNKNOWN';
  return `<span class="badge-status status-${safe.toLowerCase().replaceAll('_', '-')}">${esc(statusText[value] ?? value ?? 'Unknown')}</span>`;
}

function priorityBadge(value) {
  const safe = priorityText[value] ? value : 'UNKNOWN';
  return `<span class="badge-priority priority-${safe.toLowerCase()}">${esc(priorityText[value] ?? value ?? 'Unknown')}</span>`;
}

function parseApiError(response, text) {
  let message = text || `HTTP ${response.status}`;
  try {
    const obj = JSON.parse(text);
    message = obj.detail || obj.message || obj.error || message;
  } catch { /* Existing endpoints return plain-text errors. */ }
  return `${response.status}: ${String(message).slice(0, 220)}`;
}

async function getCsrfToken() {
    const response = await fetch('/csrf', {
        credentials: 'same-origin'
    });

    if (!response.ok) {
        throw new Error('Failed to fetch CSRF token');
    }

    return await response.json();
}

async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  headers.set('Accept', 'application/json, text/plain, */*');
  const method = (options.method || 'GET').toUpperCase();

  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
      const csrf = await getCsrfToken();
      headers.set(csrf.headerName, csrf.token);
  }
  const response = await fetch(path, {credentials: 'same-origin', ...options, headers});
  const redirectedToLogin = response.redirected && new URL(response.url).pathname === '/login.html';
  if (response.status === 401 || redirectedToLogin) {
    location.assign('/login.html');
    throw new Error('登录已过期，请重新登录');
  }
  const text = await response.text();
  if (!response.ok) throw new Error(parseApiError(response, text));
  if (!text) return null;
  if (response.headers.get('content-type')?.includes('application/json')) {
      try {
          return JSON.parse(text);
      } catch (error) {
          if ((options.method || 'GET').toUpperCase() !== 'GET') {
              return text;
          }
          throw error;
      }
  }
  return text;
  }

function queryTickets(params = {}) {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== null && value !== undefined && value !== '') query.set(key, String(value));
  }
  return api(`/tickets?${query.toString()}`);
}

function tableRows(items) {
  if (!items || !items.length) return '<tr><td colspan="6" class="table-empty">没有符合条件的工单。试着更改筛选条件。</td></tr>';
  return items.map(ticket => {
    const id = Number(ticket.id);
    if (!Number.isInteger(id)) return '';
    const assigned = ticket.assignedTo == null ? '<span class="muted">Unassigned</span>' : `<span class="assignee"><span class="assignee-dot">#</span> User ${esc(ticket.assignedTo)}</span>`;
    return `<tr>
      <td><div class="ticket-name"><div class="ticket-symbol">${icon('list')}</div><div><span class="ticket-primary" title="${esc(ticket.title)}">${esc(ticket.title)}</span><div class="ticket-sub">#${id} · ${esc(ticket.category || 'Uncategorized')}</div></div></div></td>
      <td>${statusBadge(ticket.status)}</td><td>${priorityBadge(ticket.priority)}</td><td>${assigned}</td>
      <td class="date-cell">${localDate(ticket.createdAt)}</td>
      <td class="end-cell"><button class="row-action" data-ticket="${id}" type="button">Details ${icon('chevron')}</button></td>
    </tr>`;
  }).join('');
}

function setPageHeading() {
  const mine = state.view === 'mine';
  $('ticketsHeading').innerHTML = mine ? 'Assigned to Me<span class="welcome-dot">.</span>' : 'All Tickets<span class="welcome-dot">.</span>';
  $('ticketsEyebrow').textContent = mine ? 'PERSONAL WORK QUEUE' : 'TICKET MANAGEMENT';
  $('ticketsDescription').textContent = mine ? `仅显示分配给 ${state.user.username}（User #${state.user.id}）的工单。` : '筛选、排序并管理 WorkHub 中的所有工单。';
  $('filterAssigneeWrap').hidden = mine;
}

function showView(view) {
  state.view = view;
  $('overviewView').hidden = view !== 'overview';
  $('ticketsView').hidden = view === 'overview';
  document.querySelectorAll('.nav-link').forEach(el => el.classList.toggle('is-active', el.dataset.view === view));
  $('breadcrumbCurrent').textContent = {overview: 'Overview', tickets: 'All Tickets', mine: 'Assigned to Me'}[view];
  $('sidebar').classList.remove('is-open');
  $('sidebarBackdrop').hidden = true;
  if (view === 'overview') loadOverview();
  else {
    state.page = 1;
    setPageHeading();
    loadTickets();
  }
}

function readFilters() {
  const [sortBy, order] = $('filterSort').value.split(':');
  state.filters = {
    status: $('filterStatus').value,
    priority: $('filterPriority').value,
    assignedTo: $('filterAssignee').value.trim(),
    sortBy,
    order,
    size: Number($('filterSize').value)
  };
}

function resetFilters() {
  $('filterForm').reset();
  readFilters();
  state.page = 1;
  loadTickets();
}

async function loadTickets() {
  const request = ++state.listRequest;
  $('ticketRows').innerHTML = '<tr><td colspan="6" class="table-loading">正在读取工单...</td></tr>';
  const f = state.filters;
  const assignedTo = state.view === 'mine' ? state.user.id : f.assignedTo;
  try {
    const result = await queryTickets({status: f.status, priority: f.priority, assignedTo, sortBy: f.sortBy, order: f.order, size: f.size, page: state.page});
    if (request !== state.listRequest) return;
    if (!Array.isArray(result?.data) || typeof result.total !== 'number') throw new Error('分页响应格式不正确：需要 data 和 total');
    if (state.page > Math.max(1, result.totalPages)) {
      state.page = Math.max(1, result.totalPages);
      return loadTickets();
    }
    state.result = result;
    $('ticketRows').innerHTML = tableRows(result.data);
    $('filterTotal').textContent = String(result.total);
    const from = result.total === 0 ? 0 : (result.page - 1) * result.size + 1;
    const to = Math.min(result.page * result.size, result.total);
    $('paginationSummary').textContent = `显示 ${from}–${to} 条，共 ${result.total} 条`;
    $('pageMarker').textContent = `${result.page} / ${Math.max(1, result.totalPages)}`;
    $('prevPage').disabled = result.page <= 1;
    $('nextPage').disabled = result.page >= result.totalPages;
  } catch (error) {
    if (request !== state.listRequest) return;
    $('ticketRows').innerHTML = `<tr><td colspan="6" class="table-empty">读取失败：${esc(error.message)}</td></tr>`;
    $('paginationSummary').textContent = '无法获取分页信息';
    $('filterTotal').textContent = '—';
    $('prevPage').disabled = $('nextPage').disabled = true;
    toast(`工单查询失败：${error.message}`, true);
  }
}

async function loadOverview() {
  $('recentTicketRows').innerHTML = '<tr><td colspan="6" class="table-loading">正在读取最近的工单...</td></tr>';
  const stats = [
    {name: '全部工单', key: '', iconName: 'tray', css: '', subtitle: '未删除工单'},
    {name: '待处理', key: 'OPEN', iconName: 'flag', css: 'orange', subtitle: 'Open'},
    {name: '处理中', key: 'IN_PROGRESS', iconName: 'activity', css: 'violet', subtitle: 'In progress'},
    {name: '已解决', key: 'RESOLVED', iconName: 'check', css: 'green', subtitle: 'Resolved'}
  ];
  // Summary numbers use the actual total returned by each backend filtered query.
  const totals = await Promise.allSettled(stats.map(stat => queryTickets({status: stat.key, page: 1, size: 1})));
  if (state.view !== 'overview') return;
  $('statsGrid').innerHTML = stats.map((stat, index) => {
    const item = totals[index];
    const value = item.status === 'fulfilled' ? String(item.value.total) : '—';
    return `<div class="stat-card"><div class="stat-top"><span class="stat-label">${stat.name}</span><span class="stat-icon ${stat.css}">${icon(stat.iconName)}</span></div><div class="stat-number">${value}</div><div class="stat-meta">${stat.subtitle}</div></div>`;
  }).join('');
  try {
    const latest = await queryTickets({sortBy: 'createdAt', order: 'desc', page: 1, size: 5});
    if (state.view === 'overview') $('recentTicketRows').innerHTML = tableRows(latest.data);
  } catch (error) {
    if (state.view === 'overview') $('recentTicketRows').innerHTML = `<tr><td colspan="6" class="table-empty">读取失败：${esc(error.message)}</td></tr>`;
  }
}

function closeDrawer() {
  $('drawerLayer').hidden = true;
  document.body.style.overflow = '';
  state.activeTicket = null;
}

function openCreate() {
  $('createForm').reset();
  $('createLayer').hidden = false;
  document.body.style.overflow = 'hidden';
  $('createForm').elements.title.focus();
}
function closeCreate() {
  $('createLayer').hidden = true;
  document.body.style.overflow = '';
}

async function loadTicketDetail(id) {
  $('drawerLayer').hidden = false;
  document.body.style.overflow = 'hidden';
  $('drawerContent').innerHTML = '<div class="drawer-loading">正在加载工单详情...</div>';
  $('drawerTitle').textContent = `Ticket #${id}`;
  try {
    const ticket = await api(`/tickets/${id}`);
    if (ticket == null || typeof ticket !== 'object') throw new Error('没有找到该工单');
    state.activeTicket = ticket;
    const related = await Promise.allSettled([
      api(`/tickets/${id}/comments`),
      api(`/tickets/${id}/logs`)
    ]);
    state.comments = related[0].status === 'fulfilled' ? related[0].value : [];
    state.logs = related[1].status === 'fulfilled' ? related[1].value : [];
    state.commentsError = related[0].status === 'rejected' ? related[0].reason.message : '';
    state.logsError = related[1].status === 'rejected' ? related[1].reason.message : '';
    renderDrawer();
  } catch (error) {
    $('drawerContent').innerHTML = `<div class="drawer-loading">无法读取工单：${esc(error.message)}</div>`;
    toast(error.message, true);
  }
}

function renderDrawer() {
  const ticket = state.activeTicket;
  if (!ticket) return;
  $('drawerTitle').textContent = `Ticket #${ticket.id}`;
  if (state.editing) {
    $('drawerContent').innerHTML = `<form id="editTicketForm" class="edit-form">
      <label class="field">Title <span class="required">*</span><input name="title" maxlength="150" required value="${esc(ticket.title)}"></label>
      <label class="field">Description <span class="required">*</span><textarea name="description" rows="5" required>${esc(ticket.description)}</textarea></label>
      <label class="field">Category <span class="required">*</span><select name="category" required>${categoryOptionsHtml(ticket.category)}</select></label>
      <label class="field">Resolution <span class="muted">(optional)</span><textarea name="resolution" rows="3">${esc(ticket.resolution || '')}</textarea></label>
      <div class="detail-actions"><button class="btn btn-primary" type="submit">保存修改</button><button class="btn btn-quiet" data-action="cancelEdit" type="button">取消</button></div>
      <p class="help-text">状态、优先级和负责人通过详情页单独修改。</p>
    </form>`;
    return;
  }
  const next = nextStatus[ticket.status];
  const priorityOptions = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map(p => `<option value="${p}" ${ticket.priority === p ? 'selected' : ''}>${priorityText[p]}</option>`).join('');
  const overview = `<div class="detail-top"><div><div class="detail-ticket-no">TICKET #${ticket.id}</div><h3>${esc(ticket.title)}</h3></div></div>
    <div class="detail-pills">${statusBadge(ticket.status)}${priorityBadge(ticket.priority)}</div>
    <div class="detail-section-title">Description</div><div class="detail-description">${esc(ticket.description)}</div>
    <div class="detail-grid">
      <div class="detail-item"><small>Category</small><strong>${esc(ticket.category)}</strong></div>
      <div class="detail-item"><small>Created by</small><strong>User #${esc(ticket.createdBy)}</strong></div>
      <div class="detail-item"><small>Assigned to</small><strong>${ticket.assignedTo == null ? 'Unassigned' : `User #${esc(ticket.assignedTo)}`}</strong></div>
      <div class="detail-item"><small>Created at</small><strong>${localDate(ticket.createdAt)}</strong></div>
      <div class="detail-item" style="grid-column:1/-1"><small>Resolution</small><strong>${ticket.resolution ? esc(ticket.resolution) : 'Not provided'}</strong></div>
    </div>
    <div class="detail-actions"><button data-action="edit" class="btn btn-primary" type="button">${icon('edit')} 编辑内容</button>${next ? `<button data-action="advance" class="btn btn-secondary" type="button">状态 → ${esc(statusText[next])}</button>` : ''}${ticket.status === 'RESOLVED' ? `<button data-action="reopen" class="btn btn-secondary" type="button">退回处理中</button>` : ''}<button data-action="delete" class="btn btn-danger" type="button">${icon('trash')} 删除</button></div>
    <div class="detail-section-title">Quick updates</div>
    <form id="priorityForm" class="inline-form"><label class="field">Priority<select name="priority">${priorityOptions}</select></label><button class="btn btn-outline" type="submit">更新优先级</button></form>
    <form id="assigneeForm" class="inline-form"><label class="field">Assign to user ID<input name="assignedTo" type="number" min="1" step="1" required value="${ticket.assignedTo ?? ''}" placeholder="例如 1"></label><button class="btn btn-outline" type="submit">更新负责人</button></form>
    <div class="detail-divider"></div>`;
  const tabs = `<div class="detail-tabs"><button data-tab="comments" class="detail-tab ${state.detailTab === 'comments' ? 'is-active' : ''}" type="button">Comments (${Array.isArray(state.comments) ? state.comments.length : 0})</button><button data-tab="logs" class="detail-tab ${state.detailTab === 'logs' ? 'is-active' : ''}" type="button">Status history (${Array.isArray(state.logs) ? state.logs.length : 0})</button></div>`;
  let body;
 if (state.detailTab === 'comments') {
    const items = Array.isArray(state.comments) ? state.comments : [];

    // Only the ticket creator can manage Resolution
    const canManageResolution =
        Number(ticket.createdBy) === Number(state.user.id);

    body = state.commentsError
        ? `<p class="no-results">${esc(state.commentsError)}</p>`
        : items.length
            ? items.map(c => `
                <div class="note-card">
                    <div class="note-card-top">
                        <strong>User #${esc(c.authorId)}</strong>
                        <small>${localDate(c.createdAt)}</small>
                    </div>

                    <p>${esc(c.content)}</p>

                    ${c.resolution
                        ? `<span class="badge-status status-resolved">
                               Resolution
                           </span>`
                        : ''
                    }

                    ${canManageResolution && c.resolution
                        ? `<button type="button"
                              class="btn btn-outline"
                              data-action="unmarkResolution"
                              data-comment-id="${esc(c.id)}">
                              Unmark Resolution
                          </button>`
                        : ''
                    }

                    ${canManageResolution && !c.resolution
                        ? `<button type="button"
                               class="btn btn-outline"
                               data-action="markResolution"
                               data-comment-id="${esc(c.id)}">
                               Mark as Resolution
                           </button>`
                        : ''
                    }
                </div>
            `).join('')
            : '<div class="no-results">还没有评论，添加第一条讨论。</div>';

    body += `<form id="commentForm" class="edit-form" style="margin-top:18px"><label class="field">Add comment<textarea name="content" rows="3" required maxlength="5000" placeholder="写下处理建议或工作进展..."></textarea></label><button type="submit" class="btn btn-secondary">${icon('message')} 发送评论</button></form>`;  } else {
    const items = Array.isArray(state.logs) ? state.logs : [];
    body = state.logsError ? `<p class="no-results">${esc(state.logsError)}</p>` : items.length ? items.map(log => `<div class="note-card"><div class="note-card-top"><strong>${esc(statusText[log.oldStatus] || log.oldStatus)} → ${esc(statusText[log.newStatus] || log.newStatus)}</strong><small>${localDate(log.createdAt)}</small></div></div>`).join('') : '<div class="no-results">暂无状态变更记录。</div>';
  }
  $('drawerContent').innerHTML = overview + tabs + body;
}

async function doMutation(request, successMessage, {refreshDetail = true, close = false} = {}) {
  if (state.busy) return;
  state.busy = true;
  try {
    await request();
    toast(successMessage);
    if (close) closeDrawer();
    else if (refreshDetail && state.activeTicket) {
      state.editing = false;
      await loadTicketDetail(state.activeTicket.id);
    }
    if (state.view === 'overview') await loadOverview();
    else await loadTickets();
  } catch (error) {
    toast(error.message, true);
  } finally {
    state.busy = false;
  }
}

async function createTicket(event) {
  event.preventDefault();
  if (state.busy) return;
  const form = event.currentTarget;
  const fields = new FormData(form);
  const assignedRaw = String(fields.get('assignedTo') || '').trim();
  const assignedTo = assignedRaw ? Number(assignedRaw) : null;
  const payload = {
    title: String(fields.get('title') || '').trim(),
    description: String(fields.get('description') || '').trim(),
    category: String(fields.get('category') || '').trim(),
    createdBy: state.user.id,
    assignedTo
  };
  if (!payload.title || !payload.description || !payload.category) return toast('请填写所有必填项', true);
  if (assignedTo !== null && (!Number.isInteger(assignedTo) || assignedTo <= 0)) return toast('负责人 ID 必须是正整数', true);
  state.busy = true;
  $('submitCreate').disabled = true;
  try {
    await api('/tickets', {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)});
    closeCreate();
    toast('工单创建成功');
    $('filterForm').reset();
    readFilters();
    showView('tickets');
  } catch (error) {
    toast(`创建失败：${error.message}`, true);
  } finally {
    state.busy = false;
    $('submitCreate').disabled = false;
  }
}

async function handleDrawerAction(event) {
  const tabButton = event.target.closest('[data-tab]');
  if (tabButton) {state.detailTab = tabButton.dataset.tab; renderDrawer(); return;}
  const button = event.target.closest('[data-action]');
  if (!button || !state.activeTicket) return;
  const id = state.activeTicket.id;
  switch (button.dataset.action) {
    case 'edit': state.editing = true; renderDrawer(); break;
    case 'cancelEdit': state.editing = false; renderDrawer(); break;
    case 'advance': {
      const target = nextStatus[state.activeTicket.status];
      if (!target) return;
      await doMutation(() => api(`/tickets/${id}/status`, {method: 'PUT', headers: {'Content-Type': 'text/plain;charset=UTF-8'}, body: target}), `状态已更新为 ${statusText[target]}`);
      break;
    }
    case 'reopen': {
      await doMutation(
          () => api(`/tickets/${id}/status`, {
              method: 'PUT',
              headers: {'Content-Type': 'text/plain;charset=UTF-8'},
              body: 'IN_PROGRESS'
          }),
          '工单已退回处理中'
      );
      break;
    }
    case 'markResolution': {
      const commentId = Number(button.dataset.commentId);

      await doMutation(
          () => api(
              `/tickets/${id}/comments/${commentId}/resolution`,
              { method: 'PUT' }
          ),
          'Resolution marked'
      );

      break;
  }
    case 'unmarkResolution': {
      const commentId = Number(button.dataset.commentId);

      await doMutation(
          () => api(
              `/tickets/${id}/comments/${commentId}/resolution`,
              { method: 'DELETE' }
          ),
          'Resolution unmarked'
      );

      break;
  }
    case 'delete':
      if (confirm(`确认删除 Ticket #${id}？该操作会将工单标记为已删除。`)) {
        await doMutation(() => api(`/tickets/${id}`, {method: 'DELETE'}), '工单已删除', {close: true});
      }
      break;
  }
}

async function handleDrawerSubmit(event) {
  event.preventDefault();
  const form = event.target;
  const ticket = state.activeTicket;
  if (!ticket || state.busy) return;
  const data = new FormData(form);
  if (form.id === 'editTicketForm') {
    const payload = {
      title: String(data.get('title') || '').trim(),
      description: String(data.get('description') || '').trim(),
      category: String(data.get('category') || '').trim(),
      resolution: String(data.get('resolution') || '').trim() || null
    };
    if (!payload.title || !payload.description || !payload.category) return toast('请填写所有必填项', true);
    await doMutation(() => api(`/tickets/${ticket.id}`, {method: 'PUT', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)}), '工单内容已更新');
  } else if (form.id === 'priorityForm') {
    const priority = String(data.get('priority'));
    await doMutation(() => api(`/tickets/${ticket.id}/priority`, {method: 'PUT', headers: {'Content-Type': 'text/plain;charset=UTF-8'}, body: priority}), '优先级更新成功');
  } else if (form.id === 'assigneeForm') {
    const assignedTo = Number(data.get('assignedTo'));
    if (!Number.isInteger(assignedTo) || assignedTo < 1) return toast('请输入有效的用户 ID', true);
    await doMutation(() => api(`/tickets/${ticket.id}/assignee`, {method: 'PUT', headers: {'Content-Type': 'application/json'}, body: String(assignedTo)}), '负责人更新成功');
  } else if (form.id === 'commentForm') {
    const content = String(data.get('content') || '').trim();
    if (!content) return;
    const payload = {authorId: state.user.id, content};
    await doMutation(() => api(`/tickets/${ticket.id}/comments`, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)}), '评论已发送');
  }
}

async function logout() {
    try {
        const csrf = await getCsrfToken();

        const response = await fetch('/logout', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                [csrf.headerName]: csrf.token
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        location.assign('/login.html?logout');

    } catch (error) {
        toast(`退出登录失败：${error.message}`, true);
    }
}

function wireEvents() {
  document.querySelectorAll('.nav-link').forEach(el => el.addEventListener('click', () => showView(el.dataset.view)));
  ['newTicketTop', 'newTicketList'].forEach(id => $(id).addEventListener('click', openCreate));
  ['heroBrowse', 'seeAll'].forEach(id => $(id).addEventListener('click', () => showView('tickets')));
  $('refreshOverview').addEventListener('click', loadOverview);
  $('logoutButton').addEventListener('click', logout);
  $('filterForm').addEventListener('submit', event => {event.preventDefault();readFilters();state.page = 1;loadTickets();});
  $('resetFilters').addEventListener('click', resetFilters);
  $('prevPage').addEventListener('click', () => {if (state.page > 1) {state.page--;loadTickets();}});
  $('nextPage').addEventListener('click', () => {if (state.result && state.page < state.result.totalPages) {state.page++;loadTickets();}});
  ['ticketRows', 'recentTicketRows'].forEach(id => $(id).addEventListener('click', event => {
    const button = event.target.closest('[data-ticket]');
    if (button) {state.detailTab = 'comments';state.editing = false;loadTicketDetail(Number(button.dataset.ticket));}
  }));
  $('closeDrawer').addEventListener('click', closeDrawer);
  $('drawerScrim').addEventListener('click', closeDrawer);
  $('closeCreate').addEventListener('click', closeCreate);
  $('cancelCreate').addEventListener('click', closeCreate);
  $('createScrim').addEventListener('click', closeCreate);
  $('createForm').addEventListener('submit', createTicket);
  $('drawerContent').addEventListener('click', handleDrawerAction);
  $('drawerContent').addEventListener('submit', handleDrawerSubmit);
  $('menuButton').addEventListener('click', () => {$('sidebar').classList.add('is-open');$('sidebarBackdrop').hidden = false;});
  $('sidebarBackdrop').addEventListener('click', () => {$('sidebar').classList.remove('is-open');$('sidebarBackdrop').hidden = true;});
  document.addEventListener('keydown', event => {if (event.key === 'Escape') {if (!$('createLayer').hidden) closeCreate();else if (!$('drawerLayer').hidden) closeDrawer();}});
}

async function start() {
  wireEvents();
  try {
    const user = await api('/api/me');
    if (!user || !Number.isInteger(user.id) || !user.username) throw new Error('当前用户响应格式错误');
    state.user = user;
    $('sideUsername').textContent = user.username;
    const initial = user.username.slice(0, 1).toUpperCase();
    $('sideAvatar').textContent = initial;
    $('topAvatar').textContent = initial;
    await loadOverview();
  } catch (error) {
    if (!location.pathname.includes('login')) toast(`初始化失败：${error.message}`, true);
  }
}

start();
