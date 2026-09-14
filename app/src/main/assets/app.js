const STORAGE_KEY = 'dao_leme_state_v1';
const DEFAULT_TYPES = ['干导', '看片导', '睡前导', '晨导'];
const DAILY_WORDS = [
  '记住今天，不为评判，只为看见。',
  '每一次记录，都是对自己的诚实。',
  '慢一点没关系，持续看见自己就很好。',
  '今天的选择，值得被好好记下。',
  '不必完美，只要如实记录。',
  '把今天交给今天，不提前定义明天。',
  '轻轻记录，然后继续生活。'
];
const today = () => new Date().toISOString().slice(0, 10);
const localDate = (date = new Date()) => {
  const y = date.getFullYear(); const m = String(date.getMonth() + 1).padStart(2, '0'); const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};
const parseDate = value => { const [y,m,d] = value.split('-').map(Number); return new Date(y, m - 1, d); };
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const uid = () => `type_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;

const defaultState = () => ({
  records: {},
  types: DEFAULT_TYPES.map(name => ({ id: uid(), name, active: true })),
  settings: { reminderEnabled: false, reminderTime: '23:45', theme: 'system' }
});
let state = loadState();
let route = 'checkin';
let viewMonth = new Date(new Date().getFullYear(), new Date().getMonth(), 1);
let reminderTimer;
let settingsPage = 'overview';

function loadState() {
  try { const raw = JSON.parse(localStorage.getItem(STORAGE_KEY)); if (raw?.records && raw?.types && raw?.settings) { Object.keys(raw.records).filter(date => date > localDate()).forEach(date => delete raw.records[date]); return raw; } } catch (_) {}
  return defaultState();
}
function persist() { localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); syncNativeReminder(); }
function activeTypes() { return state.types.filter(t => t.active); }
function recordFor(date) { return state.records[date] || null; }
function isFutureDate(date) { return date > localDate(); }
function dailyWord(date = localDate()) { return DAILY_WORDS[parseDate(date).getDate() % DAILY_WORDS.length]; }
function formatDate(date, options = { month: 'long', day: 'numeric', weekday: 'long' }) { return new Intl.DateTimeFormat('zh-CN', options).format(parseDate(date)); }
function showToast(message) { const el = document.getElementById('toast'); el.textContent = message; el.classList.add('show'); clearTimeout(showToast.timer); showToast.timer = setTimeout(() => el.classList.remove('show'), 1800); }
function setRoute(next) { route = next; if (next === 'settings') settingsPage = 'overview'; render(); }
function saveRecord(date, next) { if (isFutureDate(date)) return showToast('未来日期不能补记'); state.records[date] = { ...next, updatedAt: new Date().toISOString() }; persist(); scheduleReminder(); render(); showToast('已记录'); }
function setTheme(theme) { state.settings.theme = theme; persist(); applyTheme(); }
function applyTheme() {
  const dark = state.settings.theme === 'dark' || (state.settings.theme === 'system' && matchMedia('(prefers-color-scheme: dark)').matches);
  document.body.classList.toggle('theme-dark', dark); document.querySelector('meta[name="theme-color"]').content = dark ? '#121719' : '#f6f7f9';
}
function render() {
  applyTheme();
  document.getElementById('todayLabel').textContent = formatDate(localDate(), { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' });
  document.querySelectorAll('.nav-item').forEach(btn => btn.classList.toggle('active', btn.dataset.route === route));
  const main = document.getElementById('appMain');
  main.innerHTML = route === 'checkin' ? renderCheckin() : route === 'records' ? renderRecords() : route === 'analysis' ? renderAnalysis() : renderSettings();
  bindPageEvents();
}
function renderCheckin() {
  const date = localDate(); const rec = recordFor(date); const stateClass = !rec ? 'pending' : rec.state === '导了' ? 'done' : 'missed';
  return `<section class="page"><div class="hero-card">
    <div class="hero-date">${formatDate(date)}</div><p class="state-label">今天的状态</p>
    <p class="state-value ${stateClass}">${esc(rec?.state || '未记录')}</p>
    ${rec?.typeName ? `<div class="type-chip">${esc(rec.typeName)}</div>` : ''}
    <div class="action-grid" style="margin-top:22px"><button class="action action-positive" id="markDone">导了</button><button class="action action-negative" id="markMissed">没导</button></div>
  </div>
  <div class="daily-word"><span>每日一句</span><p>${esc(dailyWord(date))}</p></div>
  <div class="section"><div class="section-heading"><h2>今天的备注</h2><button class="subtle-action" id="editToday">${rec?.note ? '编辑' : '添加'}</button></div>${rec?.note ? `<div class="note-box">${esc(rec.note)}</div>` : '<p class="muted">记录一点当天的想法，最多 100 个字。</p>'}</div>
  ${rec ? `<div class="section"><button class="subtle-action" id="editTodayState">修改今天的状态</button></div>` : ''}</section>`;
}
function renderRecords() {
  const y = viewMonth.getFullYear(); const m = viewMonth.getMonth(); const first = new Date(y, m, 1); const start = (first.getDay() + 6) % 7; const days = new Date(y, m + 1, 0).getDate();
  const cells = []; for (let i = 0; i < start; i++) cells.push('<div></div>');
  for (let d = 1; d <= days; d++) { const date = localDate(new Date(y,m,d)); const rec = recordFor(date); const future = isFutureDate(date); const cls = rec ? rec.state === '导了' ? 'state-done' : 'state-missed' : 'state-pending'; cells.push(`<button class="day-cell current-month ${cls} ${future ? 'future-date' : ''} ${date === localDate() ? 'today' : ''}" data-date="${date}" ${future ? 'disabled aria-label="未来日期"' : ''}><span class="day-number">${d}</span><span class="day-mark"></span></button>`); }
  const counts = monthCounts(y,m);
  return `<section class="page"><div class="section-heading"><div><p class="eyebrow">按月查看</p><h2>记录</h2></div></div><div class="stats-grid" style="margin:16px 0"><div class="stat"><strong style="color:var(--accent)">${counts.done}</strong><span>导了</span></div><div class="stat"><strong style="color:var(--negative)">${counts.missed}</strong><span>没导</span></div><div class="stat"><strong>${counts.pending}</strong><span>未记录</span></div></div><div class="calendar-card"><div class="month-nav"><button class="month-btn" id="prevMonth">‹</button><div class="month-title">${y} 年 ${m+1} 月</div><button class="month-btn" id="nextMonth">›</button></div><div class="weekday-row"><span>一</span><span>二</span><span>三</span><span>四</span><span>五</span><span>六</span><span>日</span></div><div class="calendar-grid">${cells.join('')}</div></div></section>`;
}
function monthCounts(y,m) { const days = new Date(y,m+1,0).getDate(); let done=0, missed=0; for(let d=1;d<=days;d++){const r=recordFor(localDate(new Date(y,m,d))); if(r?.state==='导了')done++; if(r?.state==='没导')missed++;} return {done,missed,pending:days-done-missed}; }
function renderAnalysis() {
  const y = viewMonth.getFullYear(); const m = viewMonth.getMonth(); const counts = monthCounts(y,m); const typeCounts = activeAndArchivedTypeCounts(y,m); const max = Math.max(1, ...typeCounts.map(x => x.count));
  return `<section class="page"><div class="section-heading"><div><p class="eyebrow">${y} 年 ${m+1} 月</p><h2>分析</h2></div><div class="inline-row"><button class="month-btn" id="analysisPrev">‹</button><button class="month-btn" id="analysisNext">›</button></div></div><div class="stats-grid" style="margin:16px 0"><div class="stat"><strong style="color:var(--accent)">${counts.done}</strong><span>导了</span></div><div class="stat"><strong style="color:var(--negative)">${counts.missed}</strong><span>没导</span></div><div class="stat"><strong>${counts.pending}</strong><span>未记录</span></div></div><div class="section"><div class="section-heading"><h2>类型次数</h2><span class="muted">${y} 年 ${m+1} 月</span></div><div class="hero-card">${typeCounts.length ? typeCounts.map(x => `<div class="bar-row"><span>${esc(x.name)}</span><div class="bar-track"><div class="bar-fill" style="width:${Math.round(x.count/max*100)}%"></div></div><strong>${x.count}</strong></div>`).join('') : '<p class="muted">还没有类型记录。</p>'}</div></div></section>`;
}
function activeAndArchivedTypeCounts(y,m) { const map = new Map(); Object.entries(state.records).forEach(([date, r]) => { const d = parseDate(date); if (r.state === '导了' && d.getFullYear()===y && d.getMonth()===m) map.set(r.typeName, (map.get(r.typeName)||0)+1); }); return [...map].map(([name,count])=>({name,count})).sort((a,b)=>b.count-a.count); }
function settingsEntry(id, title, caption) { return `<button class="settings-entry" data-settings-page="${id}"><span><strong>${title}</strong>${caption ? `<small>${caption}</small>` : ''}</span><b>›</b></button>`; }
function renderSettings() {
  if (settingsPage === 'types') return renderTypeSettings();
  if (settingsPage === 'reminder') return renderReminderSettings();
  if (settingsPage === 'appearance') return renderAppearanceSettings();
  if (settingsPage === 'backup') return renderBackupSettings();
  if (settingsPage === 'about') return renderAboutSettings();
  return `<section class="page"><div class="section-heading"><div><p class="eyebrow">本地偏好</p><h2>设置</h2></div></div>
    <div class="settings-card">${settingsEntry('types', '类型管理', `${activeTypes().length} 个可用类型`)}${settingsEntry('reminder', '提醒设置', state.settings.reminderEnabled ? `每天 ${state.settings.reminderTime}` : '当前已关闭')}${settingsEntry('appearance', '外观设置', state.settings.theme === 'system' ? '跟随系统' : state.settings.theme === 'light' ? '明亮模式' : '暗色模式')}</div>
    <div class="settings-card">${settingsEntry('backup', '本地备份', '导入或导出 JSON 备份')}${settingsEntry('about', '关于', '导了么 1.0')}</div>
  </section>`;
}
function settingsHeader(title, caption) { return `<div class="section-heading settings-subhead"><button class="back-button" id="backSettings">‹</button><div><p class="eyebrow">${caption}</p><h2>${title}</h2></div></div>`; }
function renderTypeSettings() {
  return `<section class="page">${settingsHeader('类型管理', '设置')}
    <div class="setting-group"><div class="setting-heading">可用类型</div><div class="type-manager">${activeTypes().map(t=>`<div class="type-manager-row" data-type-id="${t.id}"><span>${esc(t.name)}</span><span class="type-actions"><button class="tiny-button" data-edit-type="${t.id}">编辑</button><button class="tiny-button danger" data-delete-type="${t.id}">删除</button></span></div>`).join('')}</div><div class="add-type"><input class="text-input" id="newType" maxlength="20" placeholder="添加类型" /><button class="subtle-action" id="addType">添加</button></div></div></section>`;
}
function renderReminderSettings() {
  return `<section class="page">${settingsHeader('提醒设置', '设置')}
    <div class="setting-group"><div class="setting-heading">每日提醒</div><div class="setting-row"><div class="setting-copy"><strong>开启提醒</strong><span>当天未记录时提醒</span></div><input class="switch" type="checkbox" id="reminderEnabled" ${state.settings.reminderEnabled?'checked':''}></div><div class="setting-row"><div class="setting-copy"><strong>提醒时间</strong><span>默认 23:45</span></div><input class="select" type="time" id="reminderTime" value="${state.settings.reminderTime}"></div></div></section>`;
}
function renderAppearanceSettings() {
  return `<section class="page">${settingsHeader('外观设置', '设置')}
    <div class="setting-group"><div class="setting-heading">主题</div><div class="setting-row"><div class="setting-copy"><strong>显示模式</strong><span>默认跟随系统</span></div><select class="select" id="themeSelect"><option value="system" ${state.settings.theme==='system'?'selected':''}>跟随系统</option><option value="light" ${state.settings.theme==='light'?'selected':''}>明亮</option><option value="dark" ${state.settings.theme==='dark'?'selected':''}>暗色</option></select></div></div></section>`;
}
function renderBackupSettings() {
  return `<section class="page">${settingsHeader('本地备份', '设置')}
    <div class="setting-group"><div class="setting-heading">备份数据</div><div class="setting-row"><div class="setting-copy"><strong>导出备份</strong><span>JSON，包含记录、类型和提醒设置</span></div><button class="subtle-action" id="exportData">导出</button></div><div class="setting-row"><div class="setting-copy"><strong>导入备份</strong><span>合并日期记录，同日期以备份为准</span></div><button class="subtle-action" id="importData">导入</button></div></div></section>`;
}
function renderAboutSettings() {
  return `<section class="page">${settingsHeader('关于', '设置')}
    <div class="setting-group"><div class="setting-heading">应用信息</div><div class="setting-row"><div class="setting-copy"><strong>应用名称</strong><span>导了么</span></div></div><div class="setting-row"><div class="setting-copy"><strong>作者</strong><span>suger</span></div></div><div class="setting-row"><div class="setting-copy"><strong>版本</strong><span>1.0</span></div></div></div>
    <div class="setting-group"><div class="setting-heading">数据说明</div><div class="setting-row"><div class="setting-copy"><strong>本地记录</strong><span>所有记录仅保存在本机，不会上传或同步。</span></div></div></div>
    <div class="setting-group"><div class="setting-heading">联系作者</div><div class="setting-row"><div class="setting-copy"><strong>QQ 2397100743</strong><span>添加时请备注来意</span></div><button class="subtle-action" id="copyContact">复制</button></div></div></section>`;
}
function bindPageEvents() {
  document.querySelectorAll('.nav-item').forEach(btn=>btn.onclick=()=>setRoute(btn.dataset.route));
  const done = document.getElementById('markDone'); if(done) done.onclick=()=>openTypeModal(localDate());
  const missed = document.getElementById('markMissed'); if(missed) missed.onclick=()=>saveRecord(localDate(), {state:'没导', note:recordFor(localDate())?.note || ''});
  const edit = document.getElementById('editToday'); if(edit) edit.onclick=()=>openNoteModal(localDate());
  const editState = document.getElementById('editTodayState'); if(editState) editState.onclick=()=>openEditStateModal(localDate());
  const prev = document.getElementById('prevMonth'); if(prev) prev.onclick=()=>{viewMonth=new Date(viewMonth.getFullYear(),viewMonth.getMonth()-1,1);render();};
  const next = document.getElementById('nextMonth'); if(next) next.onclick=()=>{viewMonth=new Date(viewMonth.getFullYear(),viewMonth.getMonth()+1,1);render();};
  const ap = document.getElementById('analysisPrev'); if(ap) ap.onclick=()=>{viewMonth=new Date(viewMonth.getFullYear(),viewMonth.getMonth()-1,1);render();};
  const an = document.getElementById('analysisNext'); if(an) an.onclick=()=>{viewMonth=new Date(viewMonth.getFullYear(),viewMonth.getMonth()+1,1);render();};
  document.querySelectorAll('[data-date]').forEach(btn=>btn.onclick=()=>openDateModal(btn.dataset.date));
  const add = document.getElementById('addType'); if(add) add.onclick=addType;
  document.querySelectorAll('[data-edit-type]').forEach(btn=>btn.onclick=()=>editType(btn.dataset.editType));
  document.querySelectorAll('[data-delete-type]').forEach(btn=>btn.onclick=()=>deleteType(btn.dataset.deleteType));
  const toggle = document.getElementById('reminderEnabled'); if(toggle) toggle.onchange=()=>{state.settings.reminderEnabled=toggle.checked;persist();scheduleReminder();};
  const time = document.getElementById('reminderTime'); if(time) time.onchange=()=>{state.settings.reminderTime=time.value;persist();scheduleReminder();};
  const theme = document.getElementById('themeSelect'); if(theme) theme.onchange=()=>{setTheme(theme.value);render();};
  const exp = document.getElementById('exportData'); if(exp) exp.onclick=exportData;
  const imp = document.getElementById('importData'); if(imp) imp.onclick=()=>document.getElementById('importInput').click();
  const copyContact = document.getElementById('copyContact'); if(copyContact) copyContact.onclick=copyContactQQ;
  document.querySelectorAll('[data-settings-page]').forEach(btn=>btn.onclick=()=>{settingsPage=btn.dataset.settingsPage;render();});
  const backSettings = document.getElementById('backSettings'); if(backSettings) backSettings.onclick=()=>{settingsPage='overview';render();};
}
function openModal(inner) { document.getElementById('modalRoot').innerHTML=`<div class="modal-backdrop" id="modalBackdrop"><div class="modal">${inner}</div></div>`; document.getElementById('modalBackdrop').onclick=e=>{if(e.target.id==='modalBackdrop')closeModal();}; }
function closeModal(){document.getElementById('modalRoot').innerHTML='';}
function openTypeModal(date) { if (isFutureDate(date)) return showToast('未来日期不能补记'); const types=activeTypes(); openModal(`<h2>选择类型</h2><p class="muted">${formatDate(date,{year:'numeric',month:'long',day:'numeric'})}</p><div class="type-choice-grid">${types.map(t=>`<button class="type-choice" data-pick-type="${t.id}">${esc(t.name)}</button>`).join('')}</div><div class="modal-actions"><button class="subtle-action" id="cancelModal">取消</button></div>`); document.querySelectorAll('[data-pick-type]').forEach(b=>b.onclick=()=>{const t=state.types.find(x=>x.id===b.dataset.pickType); saveRecord(date,{state:'导了',typeId:t.id,typeName:t.name,note:recordFor(date)?.note||''});closeModal();}); document.getElementById('cancelModal').onclick=closeModal; }
function openNoteModal(date) { if (isFutureDate(date)) return showToast('未来日期不能补记'); const rec=recordFor(date); if (!rec) return showToast('请先完成当天状态记录'); openModal(`<h2>编辑备注</h2><p class="muted">${formatDate(date,{year:'numeric',month:'long',day:'numeric'})}</p><textarea class="note-input" id="noteInput" maxlength="100" rows="4" placeholder="写下当天的想法">${esc(rec.note||'')}</textarea><div class="modal-actions"><button class="subtle-action" id="cancelModal">取消</button><button class="action action-positive" id="saveNote">保存</button></div>`); document.getElementById('cancelModal').onclick=closeModal; document.getElementById('saveNote').onclick=()=>{const value=document.getElementById('noteInput').value.trim(); saveRecord(date,{...rec,note:value}); closeModal();}; }
function openEditStateModal(date) { if (isFutureDate(date)) return showToast('未来日期不能补记'); const rec=recordFor(date) || { note: '' }; openModal(`<h2>修改状态</h2><p class="muted">${formatDate(date,{year:'numeric',month:'long',day:'numeric'})}</p><div class="action-grid"><button class="action action-positive" id="changeDone">导了</button><button class="action action-negative" id="changeMissed">没导</button></div><div class="modal-actions"><button class="subtle-action" id="cancelModal">取消</button></div>`); document.getElementById('cancelModal').onclick=closeModal; document.getElementById('changeDone').onclick=()=>{closeModal();openTypeModal(date);}; document.getElementById('changeMissed').onclick=()=>{saveRecord(date,{state:'没导',note:rec.note||''});closeModal();}; }
function openDateModal(date) { if (isFutureDate(date)) return showToast('未来日期不能补记'); const rec=recordFor(date); openModal(`<h2>${formatDate(date,{year:'numeric',month:'long',day:'numeric'})}</h2><div class="date-detail"><div><span class="muted">状态</span><div style="margin-top:5px;font-weight:700">${esc(rec?.state||'未记录')}</div></div>${rec?.typeName?`<div><span class="muted">类型</span><div style="margin-top:5px"><span class="type-chip">${esc(rec.typeName)}</span></div></div>`:''}<div><span class="muted">备注</span><div style="margin-top:5px">${rec?.note?`<div class="note-box">${esc(rec.note)}</div>`:'<span class="muted">未添加</span>'}</div></div></div><div class="modal-actions">${rec?'<button class="subtle-action" id="editDateState">修改状态</button>':'<button class="action action-positive" id="backfillDate">补记</button>'}<button class="subtle-action" id="editDateNote">备注</button></div>`); document.getElementById('editDateNote').onclick=()=>{closeModal();openNoteModal(date);}; if(rec) document.getElementById('editDateState').onclick=()=>{closeModal();openEditStateModal(date);}; else document.getElementById('backfillDate').onclick=()=>{closeModal();openEditStateModal(date);}; }
function addType(){const input=document.getElementById('newType');const name=input.value.trim();if(!name)return showToast('请输入类型名称');if(name.length>20)return showToast('类型最多 20 个字');if(state.types.some(t=>t.name===name))return showToast('类型名称不能重复');state.types.push({id:uid(),name,active:true});persist();render();showToast('类型已添加');}
function editType(id){const t=state.types.find(x=>x.id===id);if(!t||!t.active)return;openModal(`<h2>编辑类型</h2><input class="text-input" id="editTypeInput" maxlength="20" value="${esc(t.name)}" /><div class="modal-actions"><button class="subtle-action" id="cancelModal">取消</button><button class="action action-positive" id="saveType">保存</button></div>`);document.getElementById('cancelModal').onclick=closeModal;document.getElementById('saveType').onclick=()=>{const name=document.getElementById('editTypeInput').value.trim();if(!name)return showToast('请输入类型名称');if(name.length>20)return showToast('类型最多 20 个字');if(state.types.some(x=>x.id!==id&&x.name===name))return showToast('类型名称不能重复');t.name=name;Object.values(state.records).forEach(r=>{if(r.typeId===id)r.typeName=name;});persist();closeModal();render();showToast('类型已更新');};}
function deleteType(id){if(activeTypes().length<=1)return showToast('至少保留一个类型');const t=state.types.find(x=>x.id===id);if(!confirm(`删除“${t.name}”？旧记录会保留该名称。`))return;t.active=false;persist();render();showToast('类型已删除');}
function copyContactQQ(){const text='2397100743';const fallback=()=>{const input=document.createElement('textarea');input.value=text;input.setAttribute('readonly','');input.style.position='fixed';input.style.opacity='0';document.body.appendChild(input);input.select();const copied=document.execCommand('copy');input.remove();showToast(copied?'QQ号已复制':'请手动复制 QQ 号');};if(navigator.clipboard?.writeText){navigator.clipboard.writeText(text).then(()=>showToast('QQ号已复制')).catch(fallback);return;}fallback();}
function exportData(){const data=JSON.stringify({version:1,exportedAt:new Date().toISOString(),...state},null,2);if(window.AndroidBridge){window.AndroidBridge.exportBackup(data);showToast('请选择备份保存位置');return;}const blob=new Blob([data],{type:'application/json'});const a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download='DAO_Leme_backup.json';a.click();URL.revokeObjectURL(a.href);showToast('备份已导出');}
document.getElementById('importInput').onchange=async e=>{const file=e.target.files[0];if(!file)return;try{const incoming=JSON.parse(await file.text());if(!incoming.records||!incoming.types)throw new Error();const dates=Object.keys(incoming.records).filter(date=>!isFutureDate(date));const overwritten=dates.filter(d=>state.records[d]);if(!confirm(`将导入 ${dates.length} 个日期，覆盖 ${overwritten.length} 个已有日期，继续吗？`))return;dates.forEach(date=>state.records[date]=incoming.records[date]);incoming.types.forEach(it=>{if(!state.types.some(t=>t.name===it.name)){state.types.push({...it,id:uid()});}});if(incoming.settings){state.settings.reminderEnabled=!!incoming.settings.reminderEnabled;state.settings.reminderTime=incoming.settings.reminderTime||'23:45';}persist();render();showToast('备份已导入');}catch(_){showToast('备份文件无效');}e.target.value='';};
document.getElementById('themeQuick').onclick=()=>{setTheme(state.settings.theme==='dark'?'light':'dark');render();};
matchMedia('(prefers-color-scheme: dark)').addEventListener?.('change',()=>{if(state.settings.theme==='system')applyTheme();});
function syncNativeReminder(){if(!window.AndroidBridge)return;window.AndroidBridge.updateReminder(state.settings.reminderEnabled,state.settings.reminderTime);window.AndroidBridge.updateTodayRecord(localDate(),Boolean(recordFor(localDate())));}
function scheduleReminder(){clearInterval(reminderTimer);syncNativeReminder();if(window.AndroidBridge||!state.settings.reminderEnabled)return;reminderTimer=setInterval(()=>{const now=new Date();const [h,m]=state.settings.reminderTime.split(':').map(Number);if(now.getHours()===h&&now.getMinutes()===m&&!recordFor(localDate())){if('Notification' in window&&Notification.permission==='granted')new Notification('今日记录提醒');}},60000);}
render(); scheduleReminder();
if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(() => {});
