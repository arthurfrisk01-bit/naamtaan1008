/* naamtaan1008 APP — mobile PWA client.
   Consumes the public API contract (docs/api-contract.md).
   No framework, no emoji, zakka cream UI. */
(function () {
  'use strict';

  // ═══════════ utils ═══════════
  const $ = (s, el) => (el || document).querySelector(s);
  const $$ = (s, el) => Array.from((el || document).querySelectorAll(s));

  function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    const d = document.createElement('div');
    d.textContent = String(str);
    return d.innerHTML;
  }

  async function api(path, opts) {
    const res = await fetch(path, Object.assign({ headers: { 'Content-Type': 'application/json' } }, opts));
    let data = null;
    try { data = await res.json(); } catch (e) { /* non-JSON */ }
    if (!res.ok || (data && data.success === false)) {
      const msg = (data && data.error) || ('请求失败 ' + res.status);
      toast(msg);
      throw new Error(msg);
    }
    return data;
  }

  let toastTimer = null;
  function toast(msg) {
    const el = $('#toast');
    el.textContent = msg;
    el.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.hidden = true; }, 2600);
  }

  function splitShowTime(t) {
    // '2026/08/29 19:00' → { date: '2026/08/29', time: '19:00' }
    if (!t) return { date: '', time: '' };
    const m = String(t).match(/^(\S+)\s*(.*)$/);
    return m ? { date: m[1], time: m[2] } : { date: String(t), time: '' };
  }

  function nl2p(text) {
    return String(text || '').split('\n').map(p => '<p>' + escapeHtml(p) + '</p>').join('');
  }

  // ═══════════ state ═══════════
  const state = {
    content: null,
    shows: null,
    gba: null,
    focus: null,
    scene: null,
    articles: { items: [], page: 1, pageSize: 8, total: 0 },
    recruitment: null,
    sceneTab: 'bands',
    sceneCity: '',
    scenePage: 1,
    scenePageSize: 6,
    showsShown: 20,
    current: 'home'
  };

  // Local dev hits production API cross-origin (CORS is enabled); deployed app is same-origin.
  const API = (location.hostname === 'localhost' || location.hostname === '127.0.0.1')
    ? 'https://naamtaan1008.com/api'
    : '/api';

  // ═══════════ scene constants ═══════════
  const SCENE_TYPES = [
    { key: 'bands', label: '乐队', color: '#d4705a' },
    { key: 'venues', label: '场地', color: '#5a8ab5' },
    { key: 'rehearsals', label: '排练房', color: '#6bb57a' },
    { key: 'shops', label: '商店', color: '#c9a227' },
    { key: 'studios', label: '工作室', color: '#8a7ab5' },
    { key: 'homestays', label: '民宿', color: '#c77d5e' }
  ];
  const SCENE_MAP = SCENE_TYPES.reduce((m, t) => { m[t.key] = t; return m; }, {});
  const SCENE_LABELS = SCENE_TYPES.reduce((m, t) => { m[t.key] = t.label; return m; }, {});

  // Type → detail fields [key, label]. Render by type, never by field existence.
  const TYPE_FIELDS = {
    bands: [['intro', '简介'], ['styles', '风格'], ['members', '成员'], ['links', '链接'], ['contact', '联系']],
    venues: [['address', '地址'], ['capacity', '容量'], ['equipment', '设备'], ['booking', '预订']],
    rehearsals: [['address', '地址'], ['price', '价格'], ['equipment', '设备'], ['hours', '营业时间'], ['contact', '联系']],
    shops: [['address', '地址'], ['hours', '营业时间'], ['contact', '联系'], ['intro', '简介'], ['equipment', '设备'], ['styles', '风格']],
    studios: [['address', '地址'], ['price', '价格'], ['hours', '营业时间'], ['contact', '联系'], ['intro', '简介'], ['equipment', '设备']],
    homestays: [['address', '地址'], ['price', '价格'], ['booking', '预订'], ['contact', '联系'], ['equipment', '设备']]
  };
  // Editable whitelist (server-side allows intro/address/contact/price/capacity/equipment/hours/booking + members/links)
  const EDITABLE_FIELDS = [['intro', '简介'], ['address', '地址'], ['contact', '联系'], ['price', '价格'], ['capacity', '容量'], ['equipment', '设备'], ['hours', '营业时间'], ['booking', '预订']];

  const CN_MAINLAND_BOUNDS = { minLat: 3.5, maxLat: 53.6, minLng: 73.5, maxLng: 135.1 };
  const GBA_BOUNDS = [[21.5, 110.5], [25.0, 117.0]];
  function isMainlandChinaCoord(lat, lng) {
    const a = Number(lat), b = Number(lng);
    if (!isFinite(a) || !isFinite(b)) return false;
    return a >= CN_MAINLAND_BOUNDS.minLat && a <= CN_MAINLAND_BOUNDS.maxLat &&
           b >= CN_MAINLAND_BOUNDS.minLng && b <= CN_MAINLAND_BOUNDS.maxLng;
  }

  // ═══════════ data loading ═══════════
  async function loadContent(force) {
    if (state.content && !force) return state.content;
    const d = await api(API + '/content');
    state.content = d;
    return d;
  }
  async function loadShows(force) {
    if (state.shows && !force) return state.shows;
    const [shows, gba, focus] = await Promise.all([
      api(API + '/shows').catch(() => ({ shows: [] })),
      api(API + '/shows/gba').catch(() => ({ shows: [] })),
      api(API + '/shows/focus').catch(() => ({ shows: [] }))
    ]);
    state.shows = (shows && shows.shows) || [];
    state.gba = (gba && gba.shows) || [];
    state.focus = (focus && focus.shows) || [];
    return state.shows;
  }
  async function loadScene(force) {
    if (state.scene && !force) return state.scene;
    const d = await api(API + '/scene/all');
    state.scene = (d && d.data) || {};
    return state.scene;
  }
  async function loadRecruitment(force) {
    if (state.recruitment && !force) return state.recruitment;
    const d = await api(API + '/recruitment');
    state.recruitment = (d && d.data) || [];
    return state.recruitment;
  }

  // ═══════════ router ═══════════
  const TAB_PAGES = { home: '/', shows: '/shows', scene: '/scene', articles: '/articles', more: '/more' };
  const PAGE_TITLES = {
    '/': '平和日常', '/shows': '演出', '/show/': '演出详情',
    '/scene': '珠三角音乐场景', '/scene/': '场景详情',
    '/articles': '文章', '/article/': '文章',
    '/more': '更多', '/about': '关于我们', '/recruitment': '招募板',
    '/products': '杂货铺', '/contact': '联系我们', '/instructions': '使用说明'
  };

  function navigate(path) {
    window.location.hash = '#' + path;
  }

  function setHeader(title, showBack) {
    $('#headerTitle').textContent = title;
    $('#backBtn').hidden = !showBack;
  }

  function setTab(activeKey) {
    $$('.tab-btn').forEach(b => b.classList.toggle('active', b.dataset.tab === activeKey));
  }

  function showPage(id) {
    $$('.page').forEach(p => p.classList.remove('active'));
    const el = document.getElementById(id);
    if (el) el.classList.add('active');
    window.scrollTo(0, 0);
  }

  function router() {
    let hash = window.location.hash.replace(/^#/, '') || '/';
    if (!hash.startsWith('/')) hash = '/' + hash;
    const parts = hash.split('/').filter(Boolean);
    const top = '/' + (parts[0] || '');
    const topKey = TAB_PAGES[top.replace('/', '')];

    if (parts.length === 0 || top === '/') {
      state.current = 'home';
      setHeader('平和日常', false); setTab('home'); showPage('page-home');
      renderHome();
    } else if (top === '/shows') {
      state.current = 'shows';
      setHeader('演出', false); setTab('shows'); showPage('page-shows');
      renderShows();
    } else if (top === '/show' && parts[1]) {
      state.current = 'show-detail';
      setHeader('演出详情', true); setTab(null); showPage('page-show-detail');
      renderShowDetail(parts[1]);
    } else if (top === '/scene') {
      if (parts[1] && parts[2]) {
        state.current = 'scene-detail';
        setHeader('场景详情', true); setTab(null); showPage('page-scene-detail');
        renderSceneDetail(parts[1], parts[2]);
      } else {
        state.current = 'scene';
        setHeader('珠三角音乐场景', false); setTab('scene'); showPage('page-scene');
        renderScene();
      }
    } else if (top === '/articles') {
      state.current = 'articles';
      setHeader('文章', false); setTab('articles'); showPage('page-articles');
      renderArticles();
    } else if (top === '/article' && parts[1]) {
      state.current = 'article-detail';
      setHeader('文章', true); setTab(null); showPage('page-article-detail');
      renderArticleDetail(parts[1]);
    } else if (top === '/more') {
      state.current = 'more';
      setHeader('更多', false); setTab('more'); showPage('page-more');
      renderMore();
    } else if (top === '/about') {
      state.current = 'about';
      setHeader('关于我们', true); setTab(null); showPage('page-about');
      renderAbout();
    } else if (top === '/recruitment') {
      state.current = 'recruitment';
      setHeader('招募板', true); setTab(null); showPage('page-recruitment');
      renderRecruitment();
    } else if (top === '/products') {
      state.current = 'products';
      setHeader('杂货铺', true); setTab(null); showPage('page-products');
      renderProducts();
    } else if (top === '/contact') {
      state.current = 'contact';
      setHeader('联系我们', true); setTab(null); showPage('page-contact');
      renderContact();
    } else if (top === '/instructions') {
      state.current = 'instructions';
      setHeader('使用说明', true); setTab(null); showPage('page-instructions');
      renderInstructions();
    } else {
      window.location.hash = '#/';
    }
  }

  // ═══════════ home ═══════════
  async function renderHome() {
    const body = $('#homeBody');
    body.innerHTML = '<div class="loading">加载中</div>';
    try {
      const [content] = await Promise.all([loadContent(), loadShows()]);
      const focus = (state.focus && state.focus[0]) || null;
      const gba = state.gba.filter(s => s.status !== 'past');
      const hz = state.shows.filter(s => s.status !== 'past' && s.city === '惠州');
      const upcoming = gba.concat(hz).filter((s, i, arr) => arr.indexOf(s) === i).slice(0, 4);
      const articles = (content.articles || []).slice(0, 3);
      let sceneItems = [];
      try {
        const scene = await loadScene();
        const all = [];
        SCENE_TYPES.forEach(t => (scene[t.key] || []).forEach(it => all.push({ item: it, type: t.key, label: t.label })));
        sceneItems = all.sort((a, b) => String(b.item.created || '').localeCompare(String(a.item.created || ''))).slice(0, 4);
      } catch (e) { /* scene optional */ }

      let html = '';
      if (focus) {
        const ft = splitShowTime(focus.showTime);
        html += '<div class="home-hero" id="homeHero" data-slug="' + escapeHtml(focus.slug || '') + '">' +
          '<div class="home-hero-bg" style="background-image:url(\'' + escapeHtml(focus.poster || '') + '\')"></div>' +
          '<div class="home-hero-inner">' +
            '<div class="home-hero-title">' + escapeHtml(focus.displayTitle || focus.title || '') + '</div>' +
            '<div class="home-hero-meta">' + escapeHtml(ft.date + (ft.time ? ' ' + ft.time : '')) + (focus.venue ? ' · ' + escapeHtml(focus.venue) : '') + '</div>' +
            '<div class="home-hero-link">查看详情</div>' +
          '</div></div>';
      }
      html += '<div class="section-title">周边演出</div>';
      if (upcoming.length) {
        upcoming.forEach(s => { html += showCard(s); });
      } else {
        html += '<div class="empty">暂无即将上演的演出</div>';
      }
      html += '<div class="section-title">最新文章</div>';
      if (articles.length) {
        articles.forEach(a => { html += articleCard(a); });
      } else {
        html += '<div class="empty">暂无文章</div>';
      }
      html += '<div class="section-title">场景速览</div>';
      if (sceneItems.length) {
        html += '<div class="home-scene-grid">';
        sceneItems.forEach(s => {
          html += '<div class="scene-mini-card" data-type="' + s.type + '" data-id="' + escapeHtml(s.item.id) + '">' +
            '<div class="scene-mini-name"><span class="scene-mini-dot" style="background:' + SCENE_MAP[s.type].color + '"></span>' + escapeHtml(s.item.name) + '</div>' +
            '<div class="scene-mini-city">' + escapeHtml(s.item.city || '') + ' · ' + s.label + '</div>' +
          '</div>';
        });
        html += '</div>';
      } else {
        html += '<div class="empty">暂无场景数据</div>';
      }
      body.innerHTML = html;
      bindHomeEvents();
    } catch (e) {
      body.innerHTML = '<div class="empty">加载失败，请检查网络</div>';
    }
  }

  function showCard(s) {
    const t = splitShowTime(s.showTime);
    const soldCls = s.soldOut ? ' show-soldout' : '';
    return '<div class="card show-card" data-url="' + escapeHtml(s.url || '') + '" data-slug="' + escapeHtml(s.slug || '') + '">' +
      '<div class="show-poster"' + (s.poster ? ' style="background-image:url(\'' + escapeHtml(s.poster) + '\')"' : '') + '></div>' +
      '<div class="show-info">' +
        '<div class="show-title">' + escapeHtml(s.title || '') + '</div>' +
        '<div class="show-sub">' + escapeHtml(t.date + (t.time ? ' ' + t.time : '')) + (s.city ? ' · ' + escapeHtml(s.city) : '') + '</div>' +
        (s.venue ? '<div class="show-sub">' + escapeHtml(s.venue) + '</div>' : '') +
        (s.price ? '<div class="show-price' + soldCls + '">' + escapeHtml(s.price) + '</div>' : '') +
      '</div></div>';
  }

  function articleCard(a) {
    return '<div class="card article-card" data-id="' + escapeHtml(a.id) + '">' +
      '<div class="article-card-top"><span class="badge">' + escapeHtml(a.category || '文章') + '</span>' +
      '<span class="article-card-date">' + escapeHtml(a.date || '') + '</span></div>' +
      '<div class="article-card-title">' + escapeHtml(a.title || '') + '</div>' +
      (a.summary ? '<div class="article-card-summary">' + escapeHtml(a.summary) + '</div>' : '') +
    '</div>';
  }

  function bindHomeEvents() {
    const hero = $('#homeHero');
    if (hero) hero.addEventListener('click', () => { if (hero.dataset.slug) navigate('/show/' + hero.dataset.slug); });
    $$('.show-card', $('#homeBody')).forEach(c => c.addEventListener('click', () => {
      if (c.dataset.slug) navigate('/show/' + c.dataset.slug);
      else if (c.dataset.url) window.open(c.dataset.url, '_blank');
    }));
    $$('.article-card', $('#homeBody')).forEach(c => c.addEventListener('click', () => navigate('/article/' + c.dataset.id)));
    $$('.scene-mini-card', $('#homeBody')).forEach(c => c.addEventListener('click', () => navigate('/scene/' + c.dataset.type + '/' + c.dataset.id)));
  }

  // ═══════════ shows ═══════════
  const UPCOMING_BATCH = 20;
  let showsCity = '全部';
  async function renderShows() {
    const body = $('#showsBody');
    body.innerHTML = '<div class="loading">加载中</div>';
    try {
      await loadShows();
      const all = state.shows.concat(state.gba).filter((s, i, arr) => arr.indexOf(s) === i);
      const cities = ['全部'].concat(Array.from(new Set(all.map(s => s.city).filter(Boolean))).slice(0, 8));
      const filtered = showsCity === '全部' ? all : all.filter(s => s.city === showsCity);
      const upcoming = filtered.filter(s => s.status !== 'past');
      const past = filtered.filter(s => s.status === 'past');

      let html = '<div class="list-header"><div class="list-filter">' +
        cities.map(c => '<button class="filter-chip' + (c === showsCity ? ' active' : '') + '" data-city="' + escapeHtml(c) + '">' + escapeHtml(c) + '</button>').join('') +
      '</div></div>';
      html += '<div class="section-title">即将上演 (' + upcoming.length + ')</div>';
      if (upcoming.length) {
        const shown = Math.min(state.showsShown, upcoming.length);
        upcoming.slice(0, shown).forEach(s => { html += showCard(s); });
        if (shown < upcoming.length) {
          html += '<button class="btn btn-ghost btn-block" id="moreShows" style="margin-top:14px">加载更多 (' + (upcoming.length - shown) + ')</button>';
        }
      } else {
        html += '<div class="empty">该城市暂无即将上演的演出</div>';
      }
      if (past.length) {
        html += '<button class="past-toggle" id="pastToggle">已过期 (' + past.length + '场) <span class="arrow">▶</span></button>' +
          '<div class="past-list" id="pastList">' + past.map(s => '<div class="card show-card past-card" data-slug="' + escapeHtml(s.slug || '') + '" data-url="' + escapeHtml(s.url || '') + '">' +
            '<div class="show-poster"' + (s.poster ? ' style="background-image:url(\'' + escapeHtml(s.poster) + '\')"' : '') + '></div>' +
            '<div class="show-info"><div class="show-title">' + escapeHtml(s.title || '') + '</div>' +
            '<div class="show-sub">' + escapeHtml(splitShowTime(s.showTime).date) + (s.city ? ' · ' + escapeHtml(s.city) : '') + '</div></div></div>').join('') +
          '</div>';
      }
      body.innerHTML = html;

      $$('.filter-chip', body).forEach(ch => ch.addEventListener('click', () => { showsCity = ch.dataset.city; state.showsShown = UPCOMING_BATCH; renderShows(); }));
      const toggle = $('#pastToggle');
      if (toggle) toggle.addEventListener('click', () => {
        toggle.classList.toggle('open');
        $('#pastList').classList.toggle('open');
      });
      const moreBtn = $('#moreShows');
      if (moreBtn) moreBtn.addEventListener('click', () => {
        state.showsShown += UPCOMING_BATCH;
        renderShows();
      });
      $$('.show-card', body).forEach(c => c.addEventListener('click', () => {
        if (c.dataset.slug) navigate('/show/' + c.dataset.slug);
        else if (c.dataset.url) window.open(c.dataset.url, '_blank');
      }));
    } catch (e) {
      body.innerHTML = '<div class="empty">加载失败，请检查网络</div>';
    }
  }

  async function renderShowDetail(slug) {
    const body = $('#showDetailBody');
    body.innerHTML = '<div class="loading">加载中</div>';
    try {
      const d = await api(API + '/show/' + encodeURIComponent(slug));
      const s = d.show || d;
      const t = splitShowTime(s.showTime);
      let html = '';
      if (s.poster) html += '<img class="show-detail-poster" src="' + escapeHtml(s.poster) + '" alt="海报" loading="lazy">';
      html += '<h2 class="detail-title">' + escapeHtml(s.title || '') + '</h2>';
      if (s.displayTitle && s.displayTitle !== s.title) html += '<div style="color:var(--ink-soft);font-size:13px">' + escapeHtml(s.displayTitle) + '</div>';
      if (t.date || t.time) html += '<div class="show-detail-meta"><span class="badge">' + escapeHtml(t.date + (t.time ? ' ' + t.time : '')) + '</span></div>';
      if (s.city) html += '<div class="show-detail-meta">城市：' + escapeHtml(s.city) + '</div>';
      if (s.venue) html += '<div class="show-detail-meta">场地：' + escapeHtml(s.venue) + '</div>';
      if (s.price) html += '<div class="show-detail-meta">票价：' + escapeHtml(s.price) + '</div>';
      if (s.performers) html += '<div class="show-detail-meta">阵容：' + escapeHtml(s.performers) + '</div>';
      if (s.lineup && s.lineup.length) {
        html += '<div class="show-detail-label">阵容</div>';
        s.lineup.forEach(m => {
          html += '<div class="member-row"><b>' + escapeHtml(m.name || '') + '</b>' + (m.desc ? ' — ' + escapeHtml(m.desc) : '') + '</div>';
        });
      }
      if (s.description) html += '<div class="show-detail-label">详情</div><div class="rich-content">' + nl2p(s.description) + '</div>';
      if (s.notes) html += '<div class="show-detail-label">须知</div><div class="rich-content">' + nl2p(s.notes) + '</div>';
      if (s.url) html += '<a class="buy-btn" href="' + escapeHtml(s.url) + '" target="_blank" rel="noopener">购票 / 查看原文</a>';
      body.innerHTML = html || '<div class="empty">未找到该演出</div>';
    } catch (e) {
      body.innerHTML = '<div class="empty">未找到该演出</div>';
    }
  }

  // ═══════════ scene ═══════════
  let sceneMap = null;
  let leafletReady = null;
  function ensureLeaflet() {
    if (window.L) return Promise.resolve(window.L);
    if (leafletReady) return leafletReady;
    leafletReady = new Promise((resolve, reject) => {
      const css = document.createElement('link');
      css.rel = 'stylesheet';
      css.href = 'https://cdn.bootcdn.net/ajax/libs/leaflet/1.9.4/leaflet.css';
      document.head.appendChild(css);
      const script = document.createElement('script');
      script.src = 'https://cdn.bootcdn.net/ajax/libs/leaflet/1.9.4/leaflet.js';
      script.onload = () => resolve(window.L);
      script.onerror = () => { leafletReady = null; reject(new Error('地图资源加载失败')); };
      document.head.appendChild(script);
    });
    return leafletReady;
  }

  function initSceneMap() {
    if (sceneMap) return;
    const el = $('#sceneMap');
    if (!el) return;
    sceneMap = L.map(el, { center: [23.1, 113.5], zoom: 10 });
    L.tileLayer('https://webrd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
      maxZoom: 18,
      attribution: '高德地图'
    }).addTo(sceneMap);
  }

  function sceneMarkers() {
    const items = [];
    SCENE_TYPES.forEach(t => (state.scene[t.key] || []).forEach(it => {
      if (isMainlandChinaCoord(it.lat, it.lng)) items.push({ item: it, type: t.key });
    }));
    return items;
  }

  function renderSceneMarkers() {
    if (!sceneMap) return;
    sceneMap.eachLayer(l => { if (l instanceof L.CircleMarker) sceneMap.removeLayer(l); });
    const markers = sceneMarkers();
    markers.forEach(m => {
      L.circleMarker([Number(m.item.lat), Number(m.item.lng)], {
        radius: 7,
        fillColor: SCENE_MAP[m.type].color,
        color: '#fff',
        weight: 2,
        fillOpacity: 0.9
      }).addTo(sceneMap).on('click', () => navigate('/scene/' + m.type + '/' + m.item.id));
    });
    if (markers.length) {
      const bounds = markers.map(m => [Number(m.item.lat), Number(m.item.lng)]);
      sceneMap.fitBounds(bounds, { padding: [30, 30], maxZoom: 13 });
    } else {
      sceneMap.fitBounds(GBA_BOUNDS);
    }
  }

  async function renderScene() {
    const body = $('#sceneBody');
    body.innerHTML = '<div id="sceneMap"></div><div class="loading">加载场景数据中…</div>';
    try {
      await Promise.all([loadScene(), ensureLeaflet()]);
      const tabs = SCENE_TYPES.map(t =>
        '<button class="scene-tab' + (t.key === state.sceneTab ? ' active' : '') + '" data-tab="' + t.key + '">' + t.label + '</button>'
      ).join('');
      body.innerHTML = '<div id="sceneMap"></div><div class="scene-tabs">' + tabs + '</div><div id="sceneList"></div>';
      initSceneMap();
      renderSceneMarkers();
      renderSceneList();
      $$('.scene-tab', body).forEach(t => t.addEventListener('click', () => {
        state.sceneTab = t.dataset.tab;
        state.scenePage = 1;
        renderScene();
      }));
    } catch (e) {
      body.innerHTML = '<div class="empty">场景加载失败：' + escapeHtml(e.message) + '</div>';
    }
  }

  function renderSceneList() {
    const listEl = $('#sceneList');
    if (!listEl) return;
    const items = (state.scene[state.sceneTab] || []).slice();
    const cities = Array.from(new Set(items.map(i => i.city).filter(Boolean)));
    const cityFiltered = state.sceneCity ? items.filter(i => i.city === state.sceneCity) : items;
    const total = cityFiltered.length;
    const pages = Math.max(1, Math.ceil(total / state.scenePageSize));
    const page = Math.min(state.scenePage, pages);
    const pageItems = cityFiltered.slice((page - 1) * state.scenePageSize, page * state.scenePageSize);
    const type = SCENE_MAP[state.sceneTab];

    let html = '';
    if (cities.length > 1) {
      html += '<div class="list-header"><div class="list-filter">' +
        '<button class="filter-chip' + (!state.sceneCity ? ' active' : '') + '" data-city="">全部</button>' +
        cities.slice(0, 8).map(c => '<button class="filter-chip' + (c === state.sceneCity ? ' active' : '') + '" data-city="' + escapeHtml(c) + '">' + escapeHtml(c) + '</button>').join('') +
      '</div></div>';
    }
    if (pageItems.length) {
      pageItems.forEach(it => {
        const sub = it.address || it.intro || it.hours || it.price || '';
        html += '<div class="card scene-card" data-id="' + escapeHtml(it.id) + '">' +
          '<span class="scene-dot" style="background:' + type.color + '"></span>' +
          '<div class="scene-card-main"><div class="scene-card-name">' + escapeHtml(it.name) + '</div>' +
          '<div class="scene-card-sub">' + escapeHtml(it.city || '') + (sub ? ' · ' + escapeHtml(sub) : '') + '</div></div>' +
          '<span class="scene-card-arrow">›</span></div>';
      });
    } else {
      html += '<div class="empty">该分类暂无内容</div>';
    }
    if (total > state.scenePageSize) {
      html += '<div class="pager">' +
        '<button class="pager-btn" id="scenePrev" ' + (page <= 1 ? 'disabled' : '') + '>上一页</button>' +
        '<span class="pager-info">' + page + ' / ' + pages + '</span>' +
        '<button class="pager-btn" id="sceneNext" ' + (page >= pages ? 'disabled' : '') + '>下一页</button>' +
      '</div>';
    }
    listEl.innerHTML = html;

    $$('.filter-chip', listEl).forEach(ch => ch.addEventListener('click', () => {
      state.sceneCity = ch.dataset.city;
      state.scenePage = 1;
      renderSceneList();
    }));
    $$('.scene-card', listEl).forEach(c => c.addEventListener('click', () => navigate('/scene/' + state.sceneTab + '/' + c.dataset.id)));
    const prev = $('#scenePrev'), next = $('#sceneNext');
    if (prev) prev.addEventListener('click', () => { state.scenePage = page - 1; renderSceneList(); });
    if (next) next.addEventListener('click', () => { state.scenePage = page + 1; renderSceneList(); });
  }

  function sceneFieldHtml(key, label, value, color) {
    if (value === undefined || value === null || value === '') return '';
    let inner = escapeHtml(value);
    if (key === 'styles' && Array.isArray(value)) {
      inner = '<div class="tag-row">' + value.map(v => '<span class="tag">' + escapeHtml(v) + '</span>').join('') + '</div>';
    } else if (key === 'members' && Array.isArray(value)) {
      inner = value.map(m => {
        if (typeof m === 'string') return '<div class="member-row">' + escapeHtml(m) + '</div>';
        return '<div class="member-row"><b>' + escapeHtml(m.name || '') + '</b>' + (m.desc ? ' — ' + escapeHtml(m.desc) : '') + '</div>';
      }).join('');
    } else if (key === 'links' && value && typeof value === 'object') {
      inner = Object.keys(value).map(k => {
        const v = value[k];
        return '<div><a href="' + escapeHtml(v) + '" target="_blank" rel="noopener" style="color:var(--terracotta-deep)">' + escapeHtml(k) + '</a></div>';
      }).join('');
    }
    return '<div class="field-block"><div class="field-label" style="--t:' + (color || '#c77d5e') + '">' + label + '</div><div class="field-value">' + inner + '</div></div>';
  }

  async function renderSceneDetail(type, id) {
    const body = $('#sceneDetailBody');
    body.innerHTML = '<div class="loading">加载中</div>';
    try {
      await loadScene();
      const entry = (state.scene[type] || []).find(e => e.id === id);
      if (!entry) { body.innerHTML = '<div class="empty">未找到该条目</div>'; return; }
      const t = SCENE_MAP[type];
      const fields = TYPE_FIELDS[type] || [];
      let html = '<div class="detail-head"><span class="scene-dot" style="background:' + t.color + '"></span>' +
        '<div><div class="detail-title">' + escapeHtml(entry.name) + '</div>' +
        '<div class="detail-city">' + escapeHtml(entry.city || '') + ' · ' + t.label + '</div></div></div>';
      if (entry.image) html += '<img class="detail-img" src="' + escapeHtml(entry.image) + '" alt="' + escapeHtml(entry.name) + '" loading="lazy">';
      if (entry.images && entry.images.length) {
        html += '<div class="detail-img" style="overflow:hidden;position:relative"><div class="detail-img" style="margin:0;background:url(\'' + escapeHtml(entry.images[0]) + '\') center/cover;height:220px"></div></div>';
      }
      fields.forEach(([k, label]) => {
        if (k === 'intro' && !entry[k]) return;
        html += sceneFieldHtml(k, label, entry[k]);
      });
      // contact line
      if (!fields.some(f => f[0] === 'contact') && entry.contact) {
        html += sceneFieldHtml('contact', '联系', entry.contact);
      }
      // comments
      const comments = entry.comments || [];
      html += '<div class="field-block"><div class="field-label">评论 (' + comments.length + ')</div></div>';
      if (comments.length) {
        html += '<div class="card" style="padding:4px 16px">' + comments.slice().reverse().map(c =>
          '<div class="comment-item"><div class="comment-head"><span>' + escapeHtml(c.author || '匿名') + '</span><span>' + escapeHtml((c.time || '').slice(0, 10)) + '</span></div>' +
          '<div class="comment-body">' + escapeHtml(c.content) + '</div></div>'
        ).join('') + '</div>';
      }
      html += '<div class="comment-form">' +
        '<input class="input" id="commentAuthor" placeholder="昵称" maxlength="30">' +
        '<textarea class="textarea" id="commentContent" placeholder="说点什么…" maxlength="500"></textarea>' +
        '<button class="btn btn-block" id="commentSubmit">发表评论</button>' +
      '</div>';
      html += '<button class="btn btn-ghost btn-block" id="sceneEditBtn" style="margin-top:12px">编辑条目</button>' +
        '<div id="sceneEditArea" hidden></div>';
      body.innerHTML = html;

      $('#commentSubmit').addEventListener('click', async () => {
        const author = $('#commentAuthor').value.trim() || '匿名';
        const content = $('#commentContent').value.trim();
        if (!content) { toast('评论内容不能为空'); return; }
        const btn = $('#commentSubmit');
        btn.disabled = true; btn.textContent = '提交中…';
        try {
          await api(API + '/scene/' + type + '/' + encodeURIComponent(id) + '/comment', {
            method: 'POST', body: JSON.stringify({ author, content })
          });
          toast('评论成功');
          await loadScene(true);
          renderSceneDetail(type, id);
        } catch (e) { btn.disabled = false; btn.textContent = '发表评论'; }
      });
      $('#sceneEditBtn').addEventListener('click', () => {
        const area = $('#sceneEditArea');
        if (!area.hidden) { area.hidden = true; return; }
        let f = '<input class="input" id="sceneToken" type="password" placeholder="编辑口令" style="margin-top:12px">' +
          '<button class="btn btn-block" id="sceneTokenConfirm" style="margin-top:8px">验证口令</button>';
        area.innerHTML = f;
        area.hidden = false;
        $('#sceneTokenConfirm').addEventListener('click', async () => {
          const token = $('#sceneToken').value.trim();
          if (!token) { toast('请输入编辑口令'); return; }
          try {
            await api(API + '/scene/verify-token', { method: 'POST', body: JSON.stringify({ token, type, id }) });
            showSceneEditForm(type, id, entry);
          } catch (e) { /* toast shown */ }
        });
      });
    } catch (e) {
      body.innerHTML = '<div class="empty">加载失败</div>';
    }
  }

  function showSceneEditForm(type, id, entry) {
    const area = $('#sceneEditArea');
    let f = '';
    EDITABLE_FIELDS.forEach(([k, label]) => {
      const cur = entry[k];
      if (k === 'members') {
        const val = Array.isArray(cur) ? cur.map(m => (typeof m === 'string' ? m : (m.name + (m.desc ? '｜' + m.desc : '')))).join('\n') : '';
        f += '<label style="font-size:13px;color:var(--ink-soft);margin:10px 0 4px;display:block">' + label + '（每行一个，名称｜描述）</label>' +
          '<textarea class="textarea" id="edit-members" style="min-height:70px">' + escapeHtml(val) + '</textarea>';
      } else if (k === 'links') {
        const val = cur && typeof cur === 'object' ? JSON.stringify(cur, null, 2) : '';
        f += '<label style="font-size:13px;color:var(--ink-soft);margin:10px 0 4px;display:block">' + label + '（JSON，如 {"微博":"https://..."}）</label>' +
          '<textarea class="textarea" id="edit-links" style="min-height:60px;font-family:monospace;font-size:12px">' + escapeHtml(val) + '</textarea>';
      } else {
        const isLong = k === 'intro' || k === 'equipment';
        f += '<label style="font-size:13px;color:var(--ink-soft);margin:10px 0 4px;display:block">' + label + '</label>' +
          (isLong
            ? '<textarea class="textarea" id="edit-' + k + '" style="min-height:70px">' + escapeHtml(cur || '') + '</textarea>'
            : '<input class="input" id="edit-' + k + '" value="' + escapeHtml(cur || '') + '">');
      }
    });
    f += '<button class="btn btn-block" id="editSave" style="margin-top:14px">保存修改</button>' +
      '<button class="btn btn-ghost btn-block" id="editCancel" style="margin-top:8px">取消</button>';
    area.innerHTML = f;

    $('#editSave').addEventListener('click', async () => {
      const payload = { token: $('#sceneToken').value.trim() };
      EDITABLE_FIELDS.forEach(([k]) => {
        const el = $('#edit-' + k);
        if (!el) return;
        if (k === 'members') {
          payload.members = el.value.split('\n').map(s => s.trim()).filter(Boolean);
        } else if (k === 'links') {
          try { payload.links = el.value.trim() ? JSON.parse(el.value) : {}; }
          catch (e) { toast('链接 JSON 格式有误'); return; }
        } else {
          payload[k] = el.value;
        }
      });
      const btn = $('#editSave');
      btn.disabled = true; btn.textContent = '保存中…';
      try {
        await api(API + '/scene/' + type + '/' + encodeURIComponent(id) + '/update', { method: 'POST', body: JSON.stringify(payload) });
        toast('已保存');
        await loadScene(true);
        renderSceneDetail(type, id);
      } catch (e) { btn.disabled = false; btn.textContent = '保存修改'; }
    });
    $('#editCancel').addEventListener('click', () => { area.hidden = true; });
  }

  // ═══════════ articles ═══════════
  async function renderArticles() {
    const body = $('#articlesBody');
    body.innerHTML = '<div class="loading">加载中</div>';
    try {
      const d = await api(API + '/articles?page=1&pageSize=' + state.articles.pageSize);
      state.articles.items = d.articles || [];
      state.articles.total = d.total || 0;
      state.articles.page = 1;
      if (d.articles && d.articles.length) {
        body.innerHTML = d.articles.map(a => articleCard(a)).join('') +
          (d.total > d.articles.length ? '<button class="btn btn-ghost btn-block" id="loadMoreArticles" style="margin-top:14px">加载更多</button>' : '');
        bindArticleCards(body);
        const moreBtn = $('#loadMoreArticles');
        if (moreBtn) moreBtn.addEventListener('click', loadMoreArticles);
      } else {
        body.innerHTML = '<div class="empty">暂无文章</div>';
      }
    } catch (e) {
      body.innerHTML = '<div class="empty">加载失败，请检查网络</div>';
    }
  }

  async function loadMoreArticles() {
    const btn = $('#loadMoreArticles');
    if (!btn) return;
    btn.disabled = true; btn.textContent = '加载中…';
    try {
      const next = await api(API + '/articles?page=' + (state.articles.page + 1) + '&pageSize=' + state.articles.pageSize);
      state.articles.page += 1;
      state.articles.items = state.articles.items.concat(next.articles || []);
      btn.remove();
      const body = $('#articlesBody');
      const cards = document.createElement('div');
      cards.innerHTML = (next.articles || []).map(a => articleCard(a)).join('');
      while (cards.firstChild) body.appendChild(cards.firstChild);
      bindArticleCards(body);
      if (state.articles.items.length < state.articles.total) {
        const again = document.createElement('button');
        again.className = 'btn btn-ghost btn-block';
        again.id = 'loadMoreArticles';
        again.textContent = '加载更多';
        again.style.cssText = 'margin-top:14px';
        body.appendChild(again);
        again.addEventListener('click', loadMoreArticles);
      }
    } catch (e) {
      if (btn) { btn.disabled = false; btn.textContent = '加载更多'; }
    }
  }

  function bindArticleCards(root) {
    $$('.article-card', root).forEach(c => c.addEventListener('click', () => navigate('/article/' + c.dataset.id)));
  }

  async function renderArticleDetail(id) {
    const body = $('#articleDetailBody');
    body.innerHTML = '<div class="loading">加载中</div>';
    try {
      const d = await api(API + '/article/' + encodeURIComponent(id));
      const a = d.article;
      let html = '<h2 class="article-detail-title">' + escapeHtml(a.title || '') + '</h2>' +
        '<div class="article-detail-meta"><span class="badge">' + escapeHtml(a.category || '文章') + '</span><span>' + escapeHtml(a.date || '') + '</span></div>' +
        '<div class="article-content">' + (a.content || '<p>（无内容）</p>') + '</div>';
      body.innerHTML = html;
    } catch (e) {
      body.innerHTML = '<div class="empty">未找到该文章</div>';
    }
  }

  // ═══════════ more ═══════════
  function renderMore() {
    const body = $('#moreBody');
    const menus = [
      ['/about', '关于我们'],
      ['/recruitment', '招募板'],
      ['/products', '杂货铺'],
      ['/instructions', '使用说明'],
      ['/contact', '联系我们']
    ];
    let html = '<div class="menu-list">' + menus.map(m =>
      '<div class="menu-item" data-nav="' + m[0] + '"><span>' + m[1] + '</span><span class="arrow">›</span></div>'
    ).join('') + '</div>' +
    '<div class="more-site">naamtaan1008 · 平和日常<br>珠三角独立音乐场景平台</div>';
    body.innerHTML = html;
    $$('.menu-item', body).forEach(m => m.addEventListener('click', () => navigate(m.dataset.nav)));
  }

  async function renderAbout() {
    const body = $('#aboutBody');
    body.innerHTML = '<div class="loading">加载中</div>';
    try {
      const c = await loadContent();
      let html = '';
      if (c.about && c.about.content) html += '<div class="card" style="padding:16px"><div class="rich-content">' + c.about.content + '</div></div>';
      if (c.about && c.about.team && c.about.team.length) {
        html += '<div class="section-title">团队</div><div class="card" style="padding:4px 16px">' +
          c.about.team.map(m => '<div class="team-item"><div class="team-role">' + escapeHtml(m.role || '') + '</div>' +
            '<div><div class="team-name">' + escapeHtml(m.name || '') + '</div>' +
            (m.desc ? '<div class="team-desc">' + escapeHtml(m.desc) + '</div>' : '') + '</div></div>').join('') +
        '</div>';
      }
      body.innerHTML = html || '<div class="empty">暂无内容</div>';
    } catch (e) {
      body.innerHTML = '<div class="empty">加载失败</div>';
    }
  }

  async function renderRecruitment() {
    const body = $('#recruitmentBody');
    body.innerHTML = '<div class="loading">加载中</div>';
    try {
      await loadRecruitment(true);
      let html = '<button class="btn btn-block" id="recruitNewBtn">发布招募</button>';
      if (state.recruitment.length) {
        html += '<div style="margin-top:14px">' + state.recruitment.map(r =>
          '<div class="card recruit-card"><div class="recruit-card-title"><span class="badge badge-mustard">' + escapeHtml(r.type || '') + '</span> ' + escapeHtml(r.title || '') + '</div>' +
          '<div class="recruit-card-sub">' + escapeHtml(r.description || '') + '</div>' +
          '<div class="recruit-card-sub">城市：' + escapeHtml(r.city || '') + ' · 联系：' + escapeHtml(r.contact || '') + '</div>' +
          '<div class="recruit-card-time">' + escapeHtml((r.time || '').slice(0, 10)) + '</div></div>'
        ).join('') + '</div>';
      } else {
        html += '<div class="empty">暂无招募信息</div>';
      }
      body.innerHTML = html;
      $('#recruitNewBtn').addEventListener('click', () => {
        const area = document.createElement('div');
        area.className = 'card form-card';
        area.style.cssText = 'margin-top:12px';
        area.innerHTML = '<label>类型</label><select class="input" id="recruitType"><option value="找乐手">找乐手</option><option value="找乐队">找乐队</option><option value="招演出">招演出</option><option value="找演出">找演出</option><option value="其他">其他</option></select>' +
          '<label>标题</label><input class="input" id="recruitTitle" maxlength="100">' +
          '<label>详细描述</label><textarea class="textarea" id="recruitDesc" maxlength="2000"></textarea>' +
          '<label>城市</label><input class="input" id="recruitCity" maxlength="50">' +
          '<label>联系方式</label><input class="input" id="recruitContact" maxlength="200">' +
          '<label>邮箱</label><input class="input" id="recruitEmail" type="email" maxlength="100">' +
          '<button class="btn btn-block" id="recruitSubmit" style="margin-top:14px">发布</button>' +
          '<button class="btn btn-ghost btn-block" id="recruitCancel" style="margin-top:8px">取消</button>';
        body.insertBefore(area, body.firstChild.nextSibling);
        $('#recruitNewBtn').disabled = true;
        $('#recruitCancel').addEventListener('click', () => { area.remove(); $('#recruitNewBtn').disabled = false; });
        $('#recruitSubmit').addEventListener('click', async () => {
          const payload = {
            type: $('#recruitType').value,
            title: $('#recruitTitle').value.trim(),
            description: $('#recruitDesc').value.trim(),
            city: $('#recruitCity').value.trim(),
            contact: $('#recruitContact').value.trim(),
            email: $('#recruitEmail').value.trim()
          };
          if (!payload.title || !payload.description || !payload.contact || !payload.email) {
            toast('请填写标题、描述、联系方式和邮箱'); return;
          }
          const btn = $('#recruitSubmit');
          btn.disabled = true; btn.textContent = '发布中…';
          try {
            await api(API + '/recruitment/submit', { method: 'POST', body: JSON.stringify(payload) });
            toast('发布成功');
            renderRecruitment();
          } catch (e) { btn.disabled = false; btn.textContent = '发布'; }
        });
      });
    } catch (e) {
      body.innerHTML = '<div class="empty">加载失败</div>';
    }
  }

  async function renderProducts() {
    const body = $('#productsBody');
    body.innerHTML = '<div class="loading">加载中</div>';
    try {
      const c = await loadContent();
      const products = c.products || [];
      if (!products.length) { body.innerHTML = '<div class="empty">暂无商品</div>'; return; }
      let html = '<div class="product-grid">' + products.map(p =>
        '<div class="card product-card"' + (p.purchase_url ? ' data-url="' + escapeHtml(p.purchase_url) + '"' : '') + '>' +
        (p.image ? '<img class="product-img" src="' + escapeHtml(p.image) + '" alt="' + escapeHtml(p.name) + '" loading="lazy">' : '<div class="product-img" style="display:flex;align-items:center;justify-content:center;color:var(--ink-faint);font-size:12px">暂无图</div>') +
        '<div class="product-body"><div class="product-name">' + escapeHtml(p.name) + '</div>' +
        (p.price ? '<div class="product-price">' + escapeHtml(p.price) + '</div>' : '') +
        (p.category ? '<div class="product-cat">' + escapeHtml(p.category) + '</div>' : '') + '</div></div>'
      ).join('') + '</div>';
      body.innerHTML = html;
      $$('.product-card', body).forEach(c => c.addEventListener('click', () => {
        if (c.dataset.url) window.open(c.dataset.url, '_blank');
      }));
    } catch (e) {
      body.innerHTML = '<div class="empty">加载失败</div>';
    }
  }

  function renderContact() {
    const body = $('#contactBody');
    body.innerHTML = '<div class="card form-card">' +
      '<label>称呼</label><input class="input" id="ctName" maxlength="50">' +
      '<label>邮箱</label><input class="input" id="ctEmail" type="email" maxlength="100">' +
      '<label>类型</label><select class="input" id="ctType"><option value="general">留言</option><option value="feedback">问题/建议</option></select>' +
      '<label>内容</label><textarea class="textarea" id="ctMessage" maxlength="2000" placeholder="说点什么…"></textarea>' +
      '<button class="btn btn-block" id="ctSubmit" style="margin-top:16px">发送</button></div>';
    $('#ctSubmit').addEventListener('click', async () => {
      const payload = {
        name: $('#ctName').value.trim(),
        email: $('#ctEmail').value.trim(),
        message: $('#ctMessage').value.trim(),
        type: $('#ctType').value
      };
      if (!payload.name || !payload.email || !payload.message) { toast('请填写称呼、邮箱和内容'); return; }
      const btn = $('#ctSubmit');
      btn.disabled = true; btn.textContent = '发送中…';
      try {
        await api(API + '/contact/submit', { method: 'POST', body: JSON.stringify(payload) });
        toast('已发送，感谢留言');
        $('#ctName').value = ''; $('#ctEmail').value = ''; $('#ctMessage').value = '';
      } catch (e) { /* toast */ }
      btn.disabled = false; btn.textContent = '发送';
    });
  }

  async function renderInstructions() {
    const body = $('#instructionsBody');
    body.innerHTML = '<div class="loading">加载中</div>';
    try {
      const c = await loadContent();
      const ins = c.instructions || {};
      let html = '';
      if (ins.content) html += '<div class="card" style="padding:16px"><div class="rich-content">' + ins.content + '</div></div>';
      if (ins.changelog && ins.changelog.length) {
        html += '<div class="section-title">更新记录</div>';
        html += ins.changelog.map((e, i) =>
          '<div class="cl-drawer' + (i === 0 ? ' open' : '') + '">' +
            '<button class="cl-drawer-toggle"><span class="cl-drawer-date">' + escapeHtml(e.date || '') + '</span>' +
            '<span class="cl-drawer-title">' + escapeHtml(e.title || '') + '</span><span class="cl-drawer-arrow">▸</span></button>' +
            '<div class="cl-drawer-body"' + (i === 0 ? ' style="display:block"' : '') + '>' + (e.content || '') + '</div>' +
          '</div>'
        ).join('');
      }
      body.innerHTML = html || '<div class="empty">暂无内容</div>';
      $$('.cl-drawer-toggle', body).forEach(btn => btn.addEventListener('click', () => {
        const drawer = btn.parentElement;
        const bodyEl = drawer.querySelector('.cl-drawer-body');
        drawer.classList.toggle('open');
        bodyEl.style.display = drawer.classList.contains('open') ? 'block' : 'none';
      }));
    } catch (e) {
      body.innerHTML = '<div class="empty">加载失败</div>';
    }
  }

  // ═══════════ init ═══════════
  function init() {
    $('#backBtn').addEventListener('click', () => {
      history.length > 1 ? history.back() : navigate('/more');
    });
    $$('.tab-btn').forEach(b => b.addEventListener('click', () => navigate(b.dataset.nav)));
    window.addEventListener('hashchange', router);
    if ('serviceWorker' in navigator && location.protocol === 'https:') {
      navigator.serviceWorker.register('/sw.js').catch(() => { /* offline optional */ });
    }
    router();
  }

  document.addEventListener('DOMContentLoaded', init);
})();
