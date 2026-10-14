/*
 * Interactive browser demo of the Time Management for Freelancers desktop app.
 * It runs entirely on sample data in the page; nothing is sent anywhere.
 */
(function () {
  'use strict';

  const $ = (id) => document.getElementById(id);
  const $$ = (selector) => Array.prototype.slice.call(document.querySelectorAll(selector));
  const esc = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const fmt = (n) => Number(n).toFixed(2);
  const pad = (n) => (n < 10 ? '0' : '') + n;
  const iso = (d) => d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
  const addDays = (n) => { const d = new Date(); d.setDate(d.getDate() + n); return d; };
  const sum = (list, pick) => list.reduce((total, item) => total + pick(item), 0);
  const TITLES = { overview: 'Overview', time: 'Time Tracking', projects: 'My Projects', invoices: 'Invoices', leaderboard: 'Leaderboard', messages: 'Messages' };
  const CIRCUMFERENCE = 2 * Math.PI * 46;

  // ------------------------------------------------------------------ sample data
  const state = {
    projects: [
      { id: 1, name: 'Website Redesign', client: 'Acme Corp', deadline: iso(addDays(2)), status: 'In progress' },
      { id: 2, name: 'Mobile App MVP', client: 'Bright Studio', deadline: iso(addDays(21)), status: 'Not started' },
      { id: 3, name: 'Brand Guidelines', client: 'Acme Corp', deadline: iso(addDays(-3)), status: 'In progress' },
      { id: 4, name: 'SEO Audit', client: 'Bright Studio', deadline: iso(addDays(-10)), status: 'Completed' }
    ],
    entries: [],
    invoices: [
      { id: 1001, client: 'Bright Studio', project: 'SEO Audit', type: 'HOURLY', hours: 12.5, rate: 40, status: 'Paid' },
      { id: 1002, client: 'Acme Corp', project: 'Brand Guidelines', type: 'HOURLY', hours: 8, rate: 45, status: 'Pending' },
      { id: 1003, client: 'Acme Corp', project: 'Website Redesign', type: 'FIXED', hours: 10, rate: 600, status: 'Pending' }
    ],
    nextInvoice: 1004,
    others: {
      all: [['Rohan Mehta', 58], ['Lena Fischer', 41.5], ['Mateo Silva', 33], ['Aiko Tanaka', 18.5]],
      week: [['Rohan Mehta', 22], ['Lena Fischer', 12], ['Aiko Tanaka', 6], ['Mateo Silva', 3]]
    },
    week: true,
    tab: 'inbox',
    messages: [
      { who: 'Acme Corp', text: 'Could we move the review meeting to Friday?', time: '09:12', unread: true },
      { who: 'Bright Studio', text: 'Thanks for the SEO report, it was very helpful!', time: 'Yesterday', unread: true },
      { who: 'Acme Corp', text: 'Invoice received, we will pay it this week.', time: 'Mon', unread: false }
    ],
    sent: [{ who: 'Acme Corp', text: 'The draft designs are ready for review.', time: 'Tue', read: 'Read 14:05' }]
  };
  [['Website Redesign', 0, 3.5], ['Website Redesign', 1, 4], ['Website Redesign', 3, 2.5], ['Brand Guidelines', 2, 5],
    ['Brand Guidelines', 5, 3], ['SEO Audit', 9, 6], ['SEO Audit', 11, 6.5], ['Mobile App MVP', 4, 1.5]
  ].forEach((r) => state.entries.push({ project: r[0], date: iso(addDays(-r[1])), hours: r[2] }));

  // ------------------------------------------------------------------ helpers
  const projectStatus = (p) => (p.status !== 'Completed' && p.deadline < iso(new Date()) ? 'Overdue' : p.status);
  const invoiceTotal = (inv) => (inv.type === 'FIXED' ? inv.rate : inv.hours * inv.rate);
  const daysUntil = (dateText) => Math.round((new Date(dateText + 'T00:00:00') - new Date(iso(new Date()) + 'T00:00:00')) / 86400000);

  function pill(text) {
    const cls = { Paid: 'ok', Completed: 'ok', Pending: 'warn', 'In progress': 'warn', Overdue: 'bad', New: 'warn' }[text] || 'neutral';
    return '<span class="pill ' + cls + '">' + esc(text) + '</span>';
  }

  let toastTimer;
  function toast(message) {
    const t = $('toast');
    t.textContent = message;
    t.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => t.classList.remove('show'), 2800);
  }

  function countUp(el, target, decimals) {
    const from = parseFloat(el.textContent) || 0;
    let start = null;
    cancelAnimationFrame(el._raf);
    function step(ts) {
      if (start === null) { start = ts; }
      const t = Math.min(1, (ts - start) / 700);
      const eased = 1 - Math.pow(1 - t, 3);
      el.textContent = (from + (target - from) * eased).toFixed(decimals);
      if (t < 1) { el._raf = requestAnimationFrame(step); }
    }
    el._raf = requestAnimationFrame(step);
  }

  function numericOnly(input) {
    input.addEventListener('input', () => {
      input.value = input.value.replace(/[^0-9.]/g, '').replace(/(\..*)\./g, '$1');
    });
  }

  function downloadCsv(name, rows) {
    const csv = rows.map((r) => r.map((v) => '"' + String(v).replace(/"/g, '""') + '"').join(',')).join('\r\n');
    const link = document.createElement('a');
    link.href = URL.createObjectURL(new Blob(['\uFEFF' + csv], { type: 'text/csv' }));
    link.download = name;
    link.click();
    toast('Saved ' + name);
  }

  const ICONS = {
    clock: '<circle cx="32" cy="32" r="22"/><path d="M32 20v13l8 5"/>',
    doc: '<rect x="18" y="10" width="28" height="44" rx="5"/><path d="M25 24h14M25 32h14M25 40h9"/>',
    folder: '<rect x="8" y="20" width="48" height="32" rx="6"/><path d="M8 26V18a4 4 0 0 1 4-4h14l4 6"/>',
    chat: '<rect x="8" y="12" width="48" height="32" rx="8"/><path d="M20 44l-4 10 14-10M22 28h.1M32 28h.1M42 28h.1"/>'
  };
  const emptyHtml = (icon, title, hint) =>
    '<div class="empty"><svg viewBox="0 0 64 64">' + ICONS[icon] + '</svg><b>' + esc(title) + '</b>' + esc(hint) + '</div>';

  // ------------------------------------------------------------------ sortable, searchable tables
  function makeTable(opts) {
    const st = { key: null, dir: 1, q: '' };
    const mount = $(opts.mount);

    function draw() {
      const all = opts.rows();
      if (all.length === 0) { mount.innerHTML = emptyHtml(opts.icon, opts.emptyTitle, opts.emptyHint); return; }
      let rows = all.slice();
      const q = st.q.trim().toLowerCase();
      if (q) { rows = rows.filter((r) => opts.cols.some((c) => String(c.value(r)).toLowerCase().indexOf(q) >= 0)); }
      if (st.key) {
        const col = opts.cols.filter((c) => c.key === st.key)[0];
        rows.sort((a, b) => { const x = col.value(a); const y = col.value(b); return (x > y ? 1 : x < y ? -1 : 0) * st.dir; });
      }
      const head = opts.cols.map((c) => '<th data-key="' + c.key + '">' + c.label.toUpperCase() +
        (st.key === c.key ? (st.dir > 0 ? ' &#9650;' : ' &#9660;') : '') + '</th>').join('');
      const body = rows.map((r) => '<tr>' + opts.cols.map((c) => '<td>' + (c.html ? c.html(r) : esc(c.value(r))) + '</td>').join('') + '</tr>').join('');
      mount.innerHTML = '<table><thead><tr>' + head + '</tr></thead><tbody>' + body + '</tbody></table>';
    }

    mount.addEventListener('click', (e) => {
      const th = e.target.closest('th[data-key]');
      if (!th) { return; }
      const key = th.getAttribute('data-key');
      st.dir = st.key === key ? -st.dir : 1;
      st.key = key;
      draw();
    });
    $(opts.search).addEventListener('input', (e) => { st.q = e.target.value; draw(); });
    return { draw: draw };
  }

  const timeTable = makeTable({
    mount: 'timeTable', search: 'timeSearch', icon: 'clock',
    emptyTitle: 'No time logged yet', emptyHint: 'Start the timer or add hours manually.',
    rows: () => state.entries,
    cols: [
      { key: 'project', label: 'Project', value: (r) => r.project },
      { key: 'date', label: 'Date', value: (r) => r.date },
      { key: 'hours', label: 'Hours', value: (r) => r.hours, html: (r) => fmt(r.hours) }
    ]
  });
  const projectTable = makeTable({
    mount: 'projectTable', search: 'projectSearch', icon: 'folder',
    emptyTitle: 'No projects yet', emptyHint: 'Create your first project to start tracking time.',
    rows: () => state.projects,
    cols: [
      { key: 'id', label: 'ID', value: (r) => r.id },
      { key: 'name', label: 'Name', value: (r) => r.name },
      { key: 'client', label: 'Client', value: (r) => r.client },
      { key: 'deadline', label: 'Deadline', value: (r) => r.deadline },
      { key: 'status', label: 'Status', value: (r) => projectStatus(r), html: (r) => pill(projectStatus(r)) }
    ]
  });
  const invoiceTable = makeTable({
    mount: 'invTable', search: 'invSearch', icon: 'doc',
    emptyTitle: 'No invoices yet', emptyHint: 'Choose a project and period above, then generate one.',
    rows: () => state.invoices,
    cols: [
      { key: 'id', label: 'Invoice #', value: (r) => r.id },
      { key: 'type', label: 'Type', value: (r) => r.type },
      { key: 'client', label: 'Client', value: (r) => r.client },
      { key: 'project', label: 'Project', value: (r) => r.project },
      { key: 'hours', label: 'Hours', value: (r) => r.hours, html: (r) => fmt(r.hours) },
      { key: 'total', label: 'Total', value: (r) => invoiceTotal(r), html: (r) => fmt(invoiceTotal(r)) },
      { key: 'status', label: 'Status', value: (r) => r.status, html: (r) => pill(r.status) }
    ]
  });

  // ------------------------------------------------------------------ overview
  function renderOverview() {
    const hour = new Date().getHours();
    $('greeting').textContent = (hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening') + ', Sara';
    $('today').textContent = new Date().toLocaleDateString(undefined, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });

    const pending = sum(state.invoices.filter((i) => i.status !== 'Paid'), invoiceTotal);
    const paid = sum(state.invoices.filter((i) => i.status === 'Paid'), invoiceTotal);
    countUp($('statHours'), sum(state.entries, (e) => e.hours), 2);
    countUp($('statProjects'), state.projects.filter((p) => p.status !== 'Completed').length, 0);
    countUp($('statPending'), pending, 2);

    // alerts
    const alerts = [];
    state.projects.filter((p) => p.status !== 'Completed').forEach((p) => {
      const d = daysUntil(p.deadline);
      if (d < 0) { alerts.push('<div class="bad">Overdue: "' + esc(p.name) + '" was due ' + (-d) + ' day(s) ago</div>'); }
      else if (d <= 2) { alerts.push('<div class="bad">Deadline in ' + d + ' day(s): "' + esc(p.name) + '"</div>'); }
    });
    $('alerts').innerHTML = alerts.length ? alerts.join('') : '<div class="ok">No projects due within the next 3 days.</div>';

    // bar chart: hours per project
    const byProject = {};
    state.entries.forEach((e) => { byProject[e.project] = (byProject[e.project] || 0) + e.hours; });
    const names = Object.keys(byProject);
    const max = Math.max.apply(null, names.map((n) => byProject[n]).concat([1]));
    $('barChart').innerHTML = names.length ? names.map((n) =>
      '<div class="bar-col"><div class="bar-val">' + byProject[n].toFixed(1) + 'h</div><div class="bar" data-h="' +
      Math.round((byProject[n] / max) * 70) + '"></div><div>' + esc(n.length > 12 ? n.slice(0, 11) + '\u2026' : n) + '</div></div>').join('')
      : emptyHtml('clock', 'No hours yet', 'Log some time to see the chart.');
    requestAnimationFrame(() => $$('#barChart .bar').forEach((b) => { b.style.height = b.getAttribute('data-h') + '%'; }));

    // donut chart: paid vs pending
    const total = paid + pending;
    const paidLen = total > 0 ? CIRCUMFERENCE * (paid / total) : 0;
    const pendLen = total > 0 ? CIRCUMFERENCE * (pending / total) : 0;
    requestAnimationFrame(() => {
      $('segPaid').style.strokeDasharray = paidLen + ' ' + CIRCUMFERENCE;
      $('segPend').style.strokeDasharray = pendLen + ' ' + CIRCUMFERENCE;
      $('segPend').style.strokeDashoffset = String(-paidLen);
    });
    $('donutPct').textContent = (total > 0 ? Math.round((paid / total) * 100) : 0) + '%';
    $('donutLegend').innerHTML = '<div>PAID<b>' + fmt(paid) + '</b></div><div>PENDING<b>' + fmt(pending) + '</b></div>';
  }

  // ------------------------------------------------------------------ time tracking
  const timer = { id: null, secs: 0, project: null };
  const clock = (s) => pad(Math.floor(s / 3600)) + ':' + pad(Math.floor((s % 3600) / 60)) + ':' + pad(s % 60);

  function fillProjectSelects() {
    const open = state.projects.filter((p) => p.status !== 'Completed');
    $('timeProject').innerHTML = open.map((p) => '<option>' + esc(p.name) + '</option>').join('');
    $('invProject').innerHTML = state.projects.map((p) => '<option>' + esc(p.name) + '</option>').join('');
  }

  function addEntry(project, hours) {
    state.entries.unshift({ project: project, date: iso(new Date()), hours: hours });
    refresh();
  }

  // ------------------------------------------------------------------ invoices
  function rangeHours(project, from, to) {
    return sum(state.entries.filter((e) => e.project === project && (!from || e.date >= from) && (!to || e.date <= to)), (e) => e.hours);
  }
  function updatePreview() {
    const from = $('invFrom').value;
    const to = $('invTo').value;
    $('invPreview').textContent = from && to && from > to ? 'Check the dates' : fmt(rangeHours($('invProject').value, from, to)) + ' h';
  }

  // ------------------------------------------------------------------ leaderboard
  function renderLeaderboard() {
    const weekStart = iso(addDays(-6));
    const mine = sum(state.entries.filter((e) => !state.week || e.date >= weekStart), (e) => e.hours);
    const list = state.others[state.week ? 'week' : 'all'].map((o) => ({ name: o[0], hours: o[1], me: false }));
    list.push({ name: 'Sara Ahmed', hours: mine, me: true });
    list.sort((a, b) => b.hours - a.hours);
    const rank = list.findIndex((r) => r.me) + 1;
    const period = state.week ? 'this week' : 'of all time';

    $('lbSummary').textContent = 'You are #' + rank + ' of ' + list.length + ' ' + period;
    $('lbDetail').textContent = rank === 1 ? 'You are in the lead. Keep it up!' :
      fmt(list[rank - 2].hours - mine) + ' hours behind ' + list[rank - 2].name;

    const colors = ['#f59e0b', '#94a3b8', '#cd7f32'];
    const heights = [120, 88, 62];
    $('podium').innerHTML = [1, 0, 2].filter((i) => list[i]).map((i) =>
      '<div class="pod"><div class="av" style="background:' + colors[i] + '">' + esc(list[i].name[0]) + '</div><b>' + esc(list[i].name) +
      '</b>' + fmt(list[i].hours) + ' h<div class="block" data-h="' + heights[i] + '" style="background:' + colors[i] + '">' + (i + 1) + '</div></div>').join('');

    const top = Math.max(list[0].hours, 1);
    $('lbList').innerHTML = list.map((r, i) =>
      '<div class="lb-row' + (r.me ? ' me' : '') + '"><div class="rank">' + (i + 1) + '</div><div><b>' + esc(r.name) + (r.me ? ' (You)' : '') +
      '</b><div class="track"><div class="fill" data-w="' + Math.round((r.hours / top) * 100) + '"></div></div></div><b>' + fmt(r.hours) + ' h</b></div>').join('');
    requestAnimationFrame(() => {
      $$('#podium .block').forEach((b) => { b.style.height = b.getAttribute('data-h') + 'px'; });
      $$('#lbList .fill').forEach((f) => { f.style.width = f.getAttribute('data-w') + '%'; });
    });
  }

  // ------------------------------------------------------------------ messages
  function renderMessages() {
    const unread = state.messages.filter((m) => m.unread).length;
    $('msgBadge').hidden = unread === 0;
    $('msgBadge').textContent = String(unread);
    $('tabInbox').textContent = unread ? 'Inbox (' + unread + ' new)' : 'Inbox';
    $('tabInbox').classList.toggle('active', state.tab === 'inbox');
    $('tabSent').classList.toggle('active', state.tab === 'sent');

    const list = state.tab === 'inbox' ? state.messages : state.sent;
    $('msgList').innerHTML = list.length ? list.map((m) =>
      '<div class="msg"><div><b>' + esc(m.who) + '</b> <small>' + esc(m.time) + '</small>' + esc(m.text) + '</div>' +
      (state.tab === 'inbox' ? pill(m.unread ? 'New' : 'Read') : '<span class="pill ' + (m.read ? 'ok' : 'neutral') + '">' + esc(m.read || 'Delivered') + '</span>') + '</div>').join('')
      : emptyHtml('chat', 'Nothing here yet', 'Write a message above to get started.');
  }

  // ------------------------------------------------------------------ navigation and refresh
  let currentPage = 'overview';
  function refresh() {
    timeTable.draw();
    projectTable.draw();
    invoiceTable.draw();
    if (currentPage === 'overview') { renderOverview(); }
    if (currentPage === 'leaderboard') { renderLeaderboard(); }
    updatePreview();
  }

  function showPage(name) {
    currentPage = name;
    $$('.page').forEach((p) => {
      const on = p.id === 'page-' + name;
      p.classList.remove('active');
      if (on) { void p.offsetWidth; p.classList.add('active'); }
    });
    $$('.nav-btn[data-page]').forEach((b) => b.classList.toggle('active', b.getAttribute('data-page') === name));
    $('pageTitle').textContent = TITLES[name];
    refresh();
    if (name === 'messages') {
      renderMessages();
      setTimeout(() => { state.messages.forEach((m) => { m.unread = false; }); if (currentPage === 'messages') { renderMessages(); } }, 1500);
    }
  }

  function setTheme(dark) {
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
    $('themeBtn').textContent = dark ? 'Light mode' : 'Dark mode';
    try { localStorage.setItem('fm-theme', dark ? 'dark' : 'light'); } catch (e) { /* storage may be blocked */ }
  }

  // ------------------------------------------------------------------ wiring
  $('nav').addEventListener('click', (e) => {
    const button = e.target.closest('[data-page]');
    if (button) { showPage(button.getAttribute('data-page')); }
  });
  $('themeBtn').addEventListener('click', () => setTheme(document.documentElement.getAttribute('data-theme') !== 'dark'));

  $('startBtn').addEventListener('click', () => {
    timer.project = $('timeProject').value;
    timer.secs = 0;
    $('timerDisplay').textContent = clock(0);
    timer.id = setInterval(() => { timer.secs += 1; $('timerDisplay').textContent = clock(timer.secs); }, 1000);
    $('startBtn').disabled = true;
    $('stopBtn').disabled = false;
    $('timeProject').disabled = true;
  });
  $('stopBtn').addEventListener('click', () => {
    clearInterval(timer.id);
    timer.id = null;
    $('startBtn').disabled = false;
    $('stopBtn').disabled = true;
    $('timeProject').disabled = false;
    addEntry(timer.project, Math.max(timer.secs / 3600, 0.01));
    toast('Time entry saved for ' + timer.project);
  });
  numericOnly($('manualHours'));
  $('addManual').addEventListener('click', () => {
    const hours = parseFloat($('manualHours').value);
    if (!(hours > 0)) { toast('Enter a positive number of hours'); return; }
    addEntry($('timeProject').value, hours);
    $('manualHours').value = '';
    toast('Logged ' + fmt(hours) + ' hours');
  });
  $('exportTime').addEventListener('click', () =>
    downloadCsv('timesheet.csv', [['Date', 'Project', 'Hours']].concat(state.entries.map((e) => [e.date, e.project, fmt(e.hours)]))));

  numericOnly($('invRate'));
  ['invProject', 'invFrom', 'invTo'].forEach((id) => $(id).addEventListener('input', updatePreview));
  $('invGenerate').addEventListener('click', () => {
    const project = $('invProject').value;
    const rate = parseFloat($('invRate').value);
    const from = $('invFrom').value;
    const to = $('invTo').value;
    if (!(rate > 0)) { toast('Enter a positive rate or amount'); return; }
    if (from && to && from > to) { toast('The start date must not be after the end date'); return; }
    const type = $('invType').value;
    const hours = rangeHours(project, from, to);
    if (type === 'HOURLY' && hours <= 0) { toast('No time was logged on this project in that period'); return; }
    const owner = state.projects.filter((p) => p.name === project)[0];
    const invoice = { id: state.nextInvoice++, client: owner.client, project: project, type: type, hours: hours, rate: rate, status: 'Pending' };
    state.invoices.unshift(invoice);
    $('invRate').value = '';
    toast('Invoice #' + invoice.id + ' sent to ' + invoice.client + ' (total ' + fmt(invoiceTotal(invoice)) + ')');
    refresh();
  });
  $('exportInv').addEventListener('click', () => downloadCsv('earnings-report.csv',
    [['Invoice #', 'Client', 'Project', 'Type', 'Hours', 'Total', 'Status']].concat(
      state.invoices.map((i) => [i.id, i.client, i.project, i.type, fmt(i.hours), fmt(invoiceTotal(i)), i.status]))));

  $('lbWeek').addEventListener('click', () => { state.week = true; $('lbWeek').classList.add('active'); $('lbAll').classList.remove('active'); renderLeaderboard(); });
  $('lbAll').addEventListener('click', () => { state.week = false; $('lbAll').classList.add('active'); $('lbWeek').classList.remove('active'); renderLeaderboard(); });

  $('tabInbox').addEventListener('click', () => { state.tab = 'inbox'; renderMessages(); });
  $('tabSent').addEventListener('click', () => { state.tab = 'sent'; renderMessages(); });
  $('msgSend').addEventListener('click', () => {
    const text = $('msgText').value.trim();
    if (!text) { toast('Write a message first'); return; }
    const message = { who: $('msgTo').value, text: text, time: 'Just now', read: null };
    state.sent.unshift(message);
    $('msgText').value = '';
    state.tab = 'sent';
    renderMessages();
    toast('Message sent to ' + message.who);
    // simulate the other person opening it a few seconds later (a read receipt)
    setTimeout(() => {
      const now = new Date();
      message.read = 'Read ' + pad(now.getHours()) + ':' + pad(now.getMinutes());
      if (currentPage === 'messages') { renderMessages(); }
    }, 3500);
  });

  // ------------------------------------------------------------------ start
  let saved = null;
  try { saved = localStorage.getItem('fm-theme'); } catch (e) { /* ignore */ }
  setTheme(saved ? saved === 'dark' : !!(window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches));
  fillProjectSelects();
  showPage('overview');
  renderMessages();
})();
