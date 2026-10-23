'use strict';

const DEFAULTS = { S1: 'https://helios:21038/api/v1', S2: 'https://helios:21138/api/v1', PAGE_SIZE: 10 };
const KEYS = { S1: 's1_url', S2: 's2_url', SORT_BY: 'sort_by', SORT_ORDER: 'sort_order', PAGE_SIZE: 'page_size' };
const FILTER_LABELS = {
    id: 'ID', name: 'Имя', 'coordinates.x': 'X', 'coordinates.y': 'Y', health: 'Здоровье',
    heartCount: 'Сердец', weaponType: 'Оружие', meleeWeapon: 'Ближний бой',
    'chapter.name': 'Орден', 'chapter.parentLegion': 'Легион', 'chapter.world': 'Мир', creationDate: 'Дата',
};

let service1Url = localStorage.getItem(KEYS.S1) || DEFAULTS.S1;
let service2Url = localStorage.getItem(KEYS.S2) || DEFAULTS.S2;
let currentPage = 0;
let currentPageSize = parseInt(localStorage.getItem(KEYS.PAGE_SIZE) || DEFAULTS.PAGE_SIZE, 10);
let currentSortBy = localStorage.getItem(KEYS.SORT_BY) || 'id';
let currentSortOrder = localStorage.getItem(KEYS.SORT_ORDER) || 'asc';
let activeFilters = {};
let quickSearchActive = false, quickSearchSubstring = '';
let totalPages = 1, totalCount = 0;

document.addEventListener('DOMContentLoaded', () => {
    initSettingsInputs();
    initQuickSearch();
    initFilterChipsDelegation();
    syncPageSizeSelect();
    loadMarines();
    loadAverageHealth();
    checkServicesHealth();
});

/* ========== utilities ========== */
const esc = s => s == null ? '' : String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#039;');
const debounce = (fn, w) => { let t; return (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), w); }; };
const stripSlash = s => s.replace(/\/+$/, '');

function switchTab(n) {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    document.getElementById(`tab-btn-${n}`)?.classList.add('active');
    document.getElementById(`tab-content-${n}`)?.classList.add('active');
}

/* ========== settings ========== */
function initSettingsInputs() {
    const a = document.getElementById('config-service1-url');
    const b = document.getElementById('config-service2-url');
    if (a) a.value = service1Url;
    if (b) b.value = service2Url;
    const l1 = document.getElementById('link-ssl-s1');
    const l2 = document.getElementById('link-ssl-s2');
    if (l1) l1.href = `${service1Url}/space-marines?pageSize=1`;
    if (l2) l2.href = `${service2Url}/starship/1/unload-all`;
}
function saveConfigAndTest() {
    const s1 = stripSlash(document.getElementById('config-service1-url').value.trim());
    const s2 = stripSlash(document.getElementById('config-service2-url').value.trim());
    service1Url = s1 || DEFAULTS.S1;
    service2Url = s2 || DEFAULTS.S2;
    localStorage.setItem(KEYS.S1, service1Url);
    localStorage.setItem(KEYS.S2, service2Url);
    initSettingsInputs();
    closeModal('settings-modal');
    showToast('Настройки сохранены', 'URL применены', 'success');
    loadMarines(); loadAverageHealth(); checkServicesHealth();
}
function presetLocalhost() {
    document.getElementById('config-service1-url').value = DEFAULTS.S1;
    document.getElementById('config-service2-url').value = DEFAULTS.S2;
}
function presetHelios() {
    document.getElementById('config-service1-url').value = 'https://helios:21038/api/v1';
    document.getElementById('config-service2-url').value = 'https://helios:21138/api/v1';
}
function resetDefaultConfig() { presetLocalhost(); }

/* ========== health ========== */
async function checkServicesHealth() {
    const s1 = document.getElementById('kpi-status-s1');
    const d1 = document.getElementById('kpi-url-s1');
    const s2 = document.getElementById('kpi-status-s2');
    const d2 = document.getElementById('kpi-url-s2');
    if (d1) d1.textContent = service1Url;
    if (d2) d2.textContent = service2Url;
    try {
        const r = await fetch(`${service1Url}/space-marines?pageSize=1`);
        s1.innerHTML = r.ok ? '<span class="dot dot-online"></span> Онлайн' : `<span class="dot dot-offline"></span> Ошибка ${r.status}`;
    } catch { s1.innerHTML = '<span class="dot dot-offline"></span> Недоступен'; }
    try {
        const r = await fetch(`${service2Url}/starship/999999/unload-all`, { method: 'POST' });
        s2.innerHTML = (r.ok || r.status < 500) ? '<span class="dot dot-online"></span> Онлайн' : `<span class="dot dot-offline"></span> Ошибка ${r.status}`;
    } catch { s2.innerHTML = '<span class="dot dot-offline"></span> Недоступен'; }
}

/* ========== load marines ========== */
async function loadMarines() {
    const tbody = document.getElementById('marines-tbody');
    tbody.innerHTML = '<tr><td colspan="13" class="table-placeholder">⏳ Загрузка...</td></tr>';
    if (quickSearchActive) return loadQuickSearchResults();

    const p = new URLSearchParams({ page: currentPage, pageSize: currentPageSize });
    if (currentSortBy) { p.set('sortBy', currentSortBy); p.set('sortOrder', currentSortOrder); }
    for (const [k, v] of Object.entries(activeFilters)) if (v) p.set(k, v);

    try {
        const r = await fetch(`${service1Url}/space-marines?${p}`);
        if (!r.ok) {
            const e = await parseErr(r);
            showToast(`Ошибка ${e.code}`, e.message, 'error', e.time);
            tbody.innerHTML = `<tr><td colspan="13" class="table-placeholder" style="color:var(--danger)">⚠️ ${esc(e.message)}</td></tr>`;
            return;
        }
        const data = await r.json();
        const items = data.spaceMarines || [];
        totalPages = data.totalPages ?? 1;
        totalCount = data.totalCount ?? items.length;
        currentPage = data.page ?? 0;
        document.getElementById('kpi-total-count').textContent = totalCount;
        renderPagination(); updatePaginationInfo();
        tbody.innerHTML = items.length
            ? items.map(renderRow).join('')
            : '<tr><td colspan="13" class="table-placeholder">Ничего не найдено</td></tr>';
    } catch (e) {
        showToast('Ошибка сети', 'Не удалось связаться с сервисом 1', 'error');
        tbody.innerHTML = `<tr><td colspan="13" class="table-placeholder" style="color:var(--danger)">Сеть недоступна. Проверьте SSL-сертификат.</td></tr>`;
    }
}

async function loadQuickSearchResults() {
    const tbody = document.getElementById('marines-tbody');
    try {
        const r = await fetch(`${service1Url}/space-marines/search/by-name?substring=${encodeURIComponent(quickSearchSubstring)}`);
        if (!r.ok) {
            const e = await parseErr(r);
            showToast(`Ошибка ${e.code}`, e.message, 'error', e.time);
            tbody.innerHTML = `<tr><td colspan="13" class="table-placeholder" style="color:var(--danger)">⚠️ ${esc(e.message)}</td></tr>`;
            return;
        }
        const items = await r.json();
        totalCount = items.length; totalPages = 1;
        document.getElementById('kpi-total-count').textContent = totalCount;
        renderPagination();
        updatePaginationInfo(`Поиск «${esc(quickSearchSubstring)}»: ${items.length}`);
        tbody.innerHTML = items.length
            ? items.map(renderRow).join('')
            : '<tr><td colspan="13" class="table-placeholder">Ничего не найдено</td></tr>';
    } catch { tbody.innerHTML = '<tr><td colspan="13" class="table-placeholder">Сеть недоступна</td></tr>'; }
}

function renderRow(m) {
    const c = m.coordinates || {};
    const ch = m.chapter || {};
    const dash = '<span style="color:var(--text-muted)">—</span>';
    return `<tr id="row-marine-${m.id}" data-name="${esc(m.name)}">
        <td><strong>#${m.id}</strong></td>
        <td><strong>${esc(m.name)}</strong></td>
        <td>${c.x ?? '-'}</td>
        <td>${c.y ?? '-'}</td>
        <td class="mono">${esc(m.creationDate || '-')}</td>
        <td><strong style="color:var(--cyan)">${m.health}</strong></td>
        <td>${m.heartCount ?? dash}</td>
        <td><span class="badge badge-weapon">${m.weaponType}</span></td>
        <td>${m.meleeWeapon ? `<span class="badge badge-melee">${m.meleeWeapon}</span>` : dash}</td>
        <td>${ch.name ? esc(ch.name) : dash}</td>
        <td>${ch.parentLegion ? esc(ch.parentLegion) : dash}</td>
        <td>${ch.world ? esc(ch.world) : dash}</td>
        <td><div class="row-actions">
            <button class="btn btn-secondary btn-sm btn-icon-only" title="Редактировать" onclick="openEditModal(${m.id})">✏️</button>
            <button class="btn btn-danger btn-sm btn-icon-only" title="Удалить" onclick="confirmDelete(${m.id})">🗑️</button>
        </div></td>
    </tr>`;
}

async function loadAverageHealth() {
    try {
        const r = await fetch(`${service1Url}/space-marines/health/average`);
        if (r.ok) {
            const v = await r.json();
            const n = Number(v).toFixed(2);
            document.getElementById('kpi-avg-health').textContent = n;
            const b = document.getElementById('special-avg-health-result');
            if (b) b.textContent = n;
        }
    } catch { document.getElementById('kpi-avg-health').textContent = '-'; }
}

/* ========== validation ========== */
function markError(fieldName, hasError) {
    const g = document.querySelector(`#marine-form [data-field="${fieldName}"]`);
    if (g) g.classList.toggle('has-error', hasError);
    return !hasError;
}

function validateMarineForm() {
    let ok = true;

    // name
    const name = document.getElementById('m-name').value.trim();
    ok = markError('name', !name) && ok;

    // health > 0
    const health = parseFloat(document.getElementById('m-health').value);
    const badHealth = !Number.isFinite(health) || health <= 0;
    ok = markError('health', badHealth) && ok;

    // heart 1..3 (если задан)
    const hRaw = document.getElementById('m-heart-count').value.trim();
    let badHeart = false;
    if (hRaw !== '') {
        const h = parseInt(hRaw, 10);
        badHeart = !Number.isInteger(h) || h < 1 || h > 3;
    }
    ok = markError('heartCount', badHeart) && ok;

    // x — целое ≤ 811
    const xRaw = document.getElementById('m-coord-x').value.trim();
    const x = parseInt(xRaw, 10);
    const badX = xRaw === '' || !Number.isInteger(x) || x > 811;
    ok = markError('x', badX) && ok;

    // y — целое
    const yRaw = document.getElementById('m-coord-y').value.trim();
    const y = parseInt(yRaw, 10);
    const badY = yRaw === '' || !Number.isInteger(y);
    ok = markError('y', badY) && ok;

    // chapter: если указан любой параметр, name обязателен
    const chN = document.getElementById('m-chapter-name').value.trim();
    const chL = document.getElementById('m-chapter-legion').value.trim();
    const chW = document.getElementById('m-chapter-world').value.trim();
    const anyChapter = chN || chL || chW;
    ok = markError('chapterName', anyChapter && !chN) && ok;

    return ok;
}

/* ========== create / edit / delete ========== */
function openCreateModal() {
    document.getElementById('modal-marine-title').textContent = 'Создание космодесантника';
    document.getElementById('marine-form-id').value = '';
    document.getElementById('marine-form').reset();
    document.querySelectorAll('#marine-form .has-error').forEach(g => g.classList.remove('has-error'));
    openModal('marine-modal');
}

async function openEditModal(id) {
    try {
        const r = await fetch(`${service1Url}/space-marines/${id}`);
        if (!r.ok) { const e = await parseErr(r); showToast(`Ошибка ${e.code}`, e.message, 'error'); return; }
        const m = await r.json();
        document.getElementById('modal-marine-title').textContent = `Редактирование #${m.id}`;
        document.getElementById('marine-form-id').value = m.id;
        document.getElementById('m-name').value = m.name || '';
        document.getElementById('m-health').value = m.health ?? '';
        document.getElementById('m-heart-count').value = m.heartCount ?? '';
        document.getElementById('m-coord-x').value = m.coordinates?.x ?? '';
        document.getElementById('m-coord-y').value = m.coordinates?.y ?? '';
        document.getElementById('m-weapon').value = m.weaponType || 'MELTAGUN';
        document.getElementById('m-melee-weapon').value = m.meleeWeapon || '';
        document.getElementById('m-chapter-name').value = m.chapter?.name || '';
        document.getElementById('m-chapter-legion').value = m.chapter?.parentLegion || '';
        document.getElementById('m-chapter-world').value = m.chapter?.world || '';
        document.querySelectorAll('#marine-form .has-error').forEach(g => g.classList.remove('has-error'));
        openModal('marine-modal');
    } catch { showToast('Ошибка', 'Не удалось загрузить', 'error'); }
}

async function handleMarineSubmit(e) {
    e.preventDefault();
    if (!validateMarineForm()) {
        showToast('Проверьте форму', 'Есть некорректные поля', 'error');
        const first = document.querySelector('#marine-form .has-error');
        if (first) {
            first.scrollIntoView({ behavior: 'smooth', block: 'center' });
            first.querySelector('input, select')?.focus();
        }
        return;
    }

    const id = document.getElementById('marine-form-id').value;
    const hRaw = document.getElementById('m-heart-count').value.trim();
    const chN = document.getElementById('m-chapter-name').value.trim();
    const chL = document.getElementById('m-chapter-legion').value.trim();
    const chW = document.getElementById('m-chapter-world').value.trim();
    const melee = document.getElementById('m-melee-weapon').value;

    const payload = {
        name: document.getElementById('m-name').value.trim(),
        coordinates: { x: parseInt(document.getElementById('m-coord-x').value, 10), y: parseInt(document.getElementById('m-coord-y').value, 10) },
        health: parseFloat(document.getElementById('m-health').value),
        heartCount: hRaw !== '' ? parseInt(hRaw, 10) : null,
        weaponType: document.getElementById('m-weapon').value,
        meleeWeapon: melee || null,
        chapter: (chN || chL || chW) ? { name: chN, parentLegion: chL || null, world: chW || null } : null,
    };

    const isEdit = Boolean(id);
    const url = isEdit ? `${service1Url}/space-marines/${id}` : `${service1Url}/space-marines`;
    const btn = document.getElementById('btn-save-marine');
    btn.disabled = true; const old = btn.textContent; btn.textContent = '⏳ Сохранение...';

    try {
        const r = await fetch(url, { method: isEdit ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
        if (r.status === 200 || r.status === 201) {
            const res = await r.json();
            closeModal('marine-modal');
            showToast(isEdit ? 'Обновлено!' : 'Создано!', `«${res.name}» (ID=${res.id})`, 'success');
            loadMarines(); loadAverageHealth();
        } else {
            const err = await parseErr(r);
            showToast(`Ошибка ${err.code}`, err.message, 'error', err.time);
        }
    } catch { showToast('Ошибка сети', 'Сохранение не удалось', 'error'); }
    finally { btn.disabled = false; btn.textContent = old; }
}

async function confirmDelete(id) {
    const row = document.getElementById(`row-marine-${id}`);
    const name = row?.dataset.name || `#${id}`;
    if (!confirm(`Удалить «${name}» (ID=${id})?`)) return;
    try {
        const r = await fetch(`${service1Url}/space-marines/${id}`, { method: 'DELETE' });
        if (r.status === 204) {
            showToast('Удалено', `Десантник #${id}`, 'success');
            loadMarines(); loadAverageHealth();
        } else {
            const err = await parseErr(r);
            showToast(`Ошибка ${err.code}`, err.message, 'error', err.time);
        }
    } catch { showToast('Ошибка сети', 'Удаление не удалось', 'error'); }
}

/* ========== sort ========== */
function handleSort(field) {
    if (currentSortBy === field) currentSortOrder = currentSortOrder === 'asc' ? 'desc' : 'asc';
    else { currentSortBy = field; currentSortOrder = 'asc'; }
    localStorage.setItem(KEYS.SORT_BY, currentSortBy);
    localStorage.setItem(KEYS.SORT_ORDER, currentSortOrder);
    document.querySelectorAll('.data-table th').forEach(th => th.classList.remove('th-sorted-asc', 'th-sorted-desc'));
    document.getElementById(`th-${field.replace('.', '-')}`)?.classList.add(currentSortOrder === 'asc' ? 'th-sorted-asc' : 'th-sorted-desc');
    if (quickSearchActive) return;
    currentPage = 0; loadMarines();
}

/* ========== pagination ========== */
function updatePaginationInfo(o) {
    const el = document.getElementById('pagination-info');
    if (o) return void (el.textContent = o);
    if (!totalCount) return void (el.textContent = 'Нет записей');
    const from = currentPage * currentPageSize + 1;
    const to = Math.min((currentPage + 1) * currentPageSize, totalCount);
    el.textContent = `Показано ${from}–${to} из ${totalCount}`;
}
function renderPagination() {
    const c = document.getElementById('pagination-pages');
    if (!c) return;
    if (quickSearchActive || totalPages <= 1) {
        c.innerHTML = '';
        ['btn-first-page','btn-prev-page','btn-next-page','btn-last-page'].forEach(id => { const e = document.getElementById(id); if (e) e.disabled = true; });
        return;
    }
    document.getElementById('btn-first-page').disabled = currentPage <= 0;
    document.getElementById('btn-prev-page').disabled = currentPage <= 0;
    document.getElementById('btn-next-page').disabled = currentPage >= totalPages - 1;
    document.getElementById('btn-last-page').disabled = currentPage >= totalPages - 1;

    const pages = buildPageList(currentPage, totalPages);
    c.innerHTML = pages.map(p => p === '…'
        ? '<span class="pagination-ellipsis">…</span>'
        : `<button class="page-btn ${p === currentPage ? 'active' : ''}" onclick="goToPage(${p})">${p + 1}</button>`
    ).join('');
}
function buildPageList(cur, total) {
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);
    const res = [0];
    const s = Math.max(1, cur - 1), e = Math.min(total - 2, cur + 1);
    if (s > 1) res.push('…');
    for (let i = s; i <= e; i++) res.push(i);
    if (e < total - 2) res.push('…');
    res.push(total - 1);
    return res;
}
function syncPageSizeSelect() { const s = document.getElementById('page-size-select'); if (s) s.value = String(currentPageSize); }
function changePageSize(n) { currentPageSize = parseInt(n, 10); localStorage.setItem(KEYS.PAGE_SIZE, currentPageSize); currentPage = 0; loadMarines(); }
function prevPage() { if (currentPage > 0) { currentPage--; loadMarines(); } }
function nextPage() { if (currentPage < totalPages - 1) { currentPage++; loadMarines(); } }
function goToPage(p) { currentPage = p; loadMarines(); }
function goToLastPage() { currentPage = Math.max(0, totalPages - 1); loadMarines(); }

/* ========== filters ========== */
function toggleFiltersPanel() {
    const p = document.getElementById('filter-panel');
    p.style.display = p.style.display === 'none' ? 'block' : 'none';
}
function applyFilters(e) {
    e.preventDefault();
    const fd = new FormData(document.getElementById('filter-form'));
    activeFilters = {};
    for (const [k, v] of fd.entries()) { const t = v.trim(); if (t !== '') activeFilters[k] = t; }
    if (quickSearchActive) deactivateQuickSearch();
    currentPage = 0; renderFilterChips(); loadMarines();
}
function resetFilters() {
    document.getElementById('filter-form').reset();
    activeFilters = {}; currentPage = 0; renderFilterChips(); loadMarines();
}
function renderFilterChips() {
    const c = document.getElementById('active-filters-chips');
    const b = document.getElementById('active-filters-count');
    const keys = Object.keys(activeFilters);
    if (!keys.length) { c.style.display = 'none'; c.innerHTML = ''; b.style.display = 'none'; return; }
    c.style.display = 'flex';
    c.innerHTML = keys.map(k => `<span class="filter-chip">
        <span class="filter-chip-key">${esc(FILTER_LABELS[k] || k)}:</span>
        <span class="filter-chip-value">${esc(activeFilters[k])}</span>
        <button class="filter-chip-remove" data-key="${esc(k)}" title="Убрать">×</button>
    </span>`).join('');
    b.textContent = keys.length; b.style.display = 'inline-flex';
}
function removeFilter(key) {
    delete activeFilters[key];
    const f = document.getElementById('filter-form').elements.namedItem(key);
    if (f) f.value = '';
    currentPage = 0; renderFilterChips(); loadMarines();
}
function initFilterChipsDelegation() {
    const c = document.getElementById('active-filters-chips');
    if (!c) return;
    c.addEventListener('click', e => {
        const btn = e.target.closest('.filter-chip-remove');
        if (btn) removeFilter(btn.dataset.key);
    });
}

/* ========== quick search ========== */
function initQuickSearch() {
    const i = document.getElementById('quick-search-substring');
    if (!i) return;
    i.addEventListener('input', debounce(() => {
        const q = i.value.trim();
        if (!q) { if (quickSearchActive) clearQuickSearch(); return; }
        quickSearchActive = true; quickSearchSubstring = q;
        document.getElementById('btn-clear-search').style.display = 'inline-flex';
        currentPage = 0; loadMarines();
    }, 300));
    i.addEventListener('keydown', e => { if (e.key === 'Escape') clearQuickSearch(); });
}
function executeQuickSearch() {
    const q = document.getElementById('quick-search-substring').value.trim();
    if (!q) return clearQuickSearch();
    quickSearchActive = true; quickSearchSubstring = q;
    document.getElementById('btn-clear-search').style.display = 'inline-flex';
    currentPage = 0; loadMarines();
}
function clearQuickSearch() {
    document.getElementById('quick-search-substring').value = '';
    document.getElementById('btn-clear-search').style.display = 'none';
    deactivateQuickSearch(); loadMarines();
}
function deactivateQuickSearch() { quickSearchActive = false; quickSearchSubstring = ''; }

/* ========== starship ops ========== */
async function handleUnloadSingle(e) {
    e.preventDefault();
    const sid = document.getElementById('single-starship-id').value.trim();
    const mid = document.getElementById('single-marine-id').value.trim();
    if (!sid || !mid) return showToast('Внимание', 'Заполните оба ID', 'error');
    appendStarshipLog(`[REQ] POST /starship/${sid}/unload/${mid}`, 'info');
    try {
        const r = await fetch(`${service2Url}/starship/${sid}/unload/${mid}`, { method: 'POST' });
        if (r.ok) {
            const d = await r.json();
            appendStarshipLog(`[RES ${r.status}] ${d.message}`, 'success');
            showToast('Успех', d.message, 'success');
        } else {
            const err = await parseErr(r);
            appendStarshipLog(`[ERR ${err.code}] ${err.message}`, 'error');
            showToast(`Ошибка ${err.code}`, err.message, 'error', err.time);
        }
    } catch (ex) {
        appendStarshipLog(`[ERR] ${ex.message}`, 'error');
        showToast('Ошибка сети', 'Сервис 2 недоступен', 'error');
    }
}
async function handleUnloadAll(e) {
    e.preventDefault();
    const sid = document.getElementById('all-starship-id').value.trim();
    if (!sid) return showToast('Внимание', 'Укажите ID', 'error');
    appendStarshipLog(`[REQ] POST /starship/${sid}/unload-all`, 'info');
    try {
        const r = await fetch(`${service2Url}/starship/${sid}/unload-all`, { method: 'POST' });
        if (r.ok) {
            const d = await r.json();
            appendStarshipLog(`[RES ${r.status}] ${d.message}`, 'success');
            showToast('Готово', d.message, 'success');
        } else {
            const err = await parseErr(r);
            appendStarshipLog(`[ERR ${err.code}] ${err.message}`, 'error');
            showToast(`Ошибка ${err.code}`, err.message, 'error', err.time);
        }
    } catch (ex) {
        appendStarshipLog(`[ERR] ${ex.message}`, 'error');
        showToast('Ошибка сети', 'Сервис 2 недоступен', 'error');
    }
}
function appendStarshipLog(t, type = 'info') {
    const term = document.getElementById('starship-terminal-logs');
    const d = document.createElement('div');
    d.className = `log-entry log-${type}`;
    d.textContent = `[${new Date().toLocaleTimeString()}] ${t}`;
    term.appendChild(d); term.scrollTop = term.scrollHeight;
}
function clearStarshipLogs() {
    document.getElementById('starship-terminal-logs').innerHTML = '<div class="log-entry log-info">[SYSTEM] Журнал очищен.</div>';
}

/* ========== analytics ========== */
async function calculateAverageHealth() {
    try {
        const r = await fetch(`${service1Url}/space-marines/health/average`);
        if (r.ok) {
            const v = await r.json();
            const f = Number(v).toFixed(2);
            document.getElementById('special-avg-health-result').textContent = f;
            document.getElementById('kpi-avg-health').textContent = f;
            showToast('Успешно', `Среднее: ${f}`, 'success');
        } else { const e = await parseErr(r); showToast(`Ошибка ${e.code}`, e.message, 'error'); }
    } catch { showToast('Ошибка сети', 'Не удалось', 'error'); }
}
async function executeSpecialSearch() {
    const s = document.getElementById('special-search-substring').value.trim();
    const a = document.getElementById('special-search-results');
    if (!s) return showToast('Внимание', 'Введите подстроку', 'error');
    a.innerHTML = '<div class="muted">Поиск...</div>';
    try {
        const r = await fetch(`${service1Url}/space-marines/search/by-name?substring=${encodeURIComponent(s)}`);
        if (r.ok) {
            const list = await r.json();
            if (!list.length) return void (a.innerHTML = '<div class="muted">Ничего не найдено</div>');
            a.innerHTML = list.map(m => `<div class="mini-result-item"><span><strong>#${m.id} ${esc(m.name)}</strong> (hp ${m.health})</span><span class="badge badge-weapon">${m.weaponType}</span></div>`).join('');
            showToast('Найдено', `${list.length}`, 'success');
        } else { const e = await parseErr(r); showToast(`Ошибка ${e.code}`, e.message, 'error', e.time); a.innerHTML = `<div style="color:var(--danger)">${esc(e.message)}</div>`; }
    } catch { showToast('Ошибка сети', 'Не удалось', 'error'); }
}
async function executeSpecialHealthFilter() {
    const t = document.getElementById('special-health-threshold').value.trim();
    const a = document.getElementById('special-health-results');
    if (t === '') return showToast('Внимание', 'Укажите порог', 'error');
    a.innerHTML = '<div class="muted">Фильтрация...</div>';
    try {
        const r = await fetch(`${service1Url}/space-marines/health/greater-than?threshold=${encodeURIComponent(t)}`);
        if (r.ok) {
            const list = await r.json();
            if (!list.length) return void (a.innerHTML = `<div class="muted">Нет объектов с hp &gt; ${esc(t)}</div>`);
            a.innerHTML = list.map(m => `<div class="mini-result-item"><span><strong>#${m.id} ${esc(m.name)}</strong> (hp <span style="color:var(--cyan)">${m.health}</span>)</span><span class="badge badge-weapon">${m.weaponType}</span></div>`).join('');
            showToast('Найдено', `${list.length}`, 'success');
        } else { const e = await parseErr(r); showToast(`Ошибка ${e.code}`, e.message, 'error', e.time); a.innerHTML = `<div style="color:var(--danger)">${esc(e.message)}</div>`; }
    } catch { showToast('Ошибка сети', 'Не удалось', 'error'); }
}

/* ========== modal & toast ========== */
function openModal(id) { const e = document.getElementById(id); if (e) { e.style.display = 'flex'; document.body.style.overflow = 'hidden'; } }
function closeModal(id) { const e = document.getElementById(id); if (e) { e.style.display = 'none'; document.body.style.overflow = ''; } }

function showToast(title, msg, type = 'info', timeStr = null) {
    const c = document.getElementById('toast-container');
    const t = document.createElement('div');
    t.className = `toast toast-${type}`;
    t.innerHTML = `<div class="toast-title"><span>${esc(title)}</span><span class="toast-time">${esc(timeStr || new Date().toLocaleTimeString())}</span></div><div class="toast-msg">${esc(msg)}</div>`;
    c.appendChild(t);
    setTimeout(() => {
        t.style.opacity = '0'; t.style.transform = 'translateX(50px)'; t.style.transition = 'all .3s ease';
        setTimeout(() => t.remove(), 300);
    }, 4500);
}

async function parseErr(r) {
    try {
        const j = await r.json();
        return { code: j.code || r.status, message: j.message || `HTTP ${r.status}`, time: j.time || new Date().toLocaleTimeString() };
    } catch {
        return { code: r.status, message: r.statusText || `Ошибка ${r.status}`, time: new Date().toLocaleTimeString() };
    }
}
