'use strict';

// ════════════════════════════════════════════════════════════
//  État global
// ════════════════════════════════════════════════════════════
const G = {
  trialId:          null,
  state:            null,   // GameStateDTO courant
  actionsLeft:      0,      // actions restantes dans la phase
  selectedWitness:  -1,     // index du témoin sélectionné (défense)
  confrontSel:      [],     // indices des témoins pour confrontation
};

const PHASE_MAX_ACTIONS = {
  PROSECUTION_CASE:  3,
  DEFENSE_CASE:      5,
  CROSS_EXAMINATION: 2,
};

const PHASE_LABELS = [
  'Ouverture', 'Accusation', 'Défense', 'Contre-interrogatoire', 'Plaidoirie', 'Verdict'
];

const CRIME_FR = {
  MURDER: 'Meurtre', FRAUD: 'Fraude',
  THEFT: 'Vol', ASSAULT: 'Agression', CORRUPTION: 'Corruption'
};

// ════════════════════════════════════════════════════════════
//  API
// ════════════════════════════════════════════════════════════
async function api(method, path, body) {
  const opts = {
    method,
    headers: { 'Content-Type': 'application/json' },
  };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch('/api/game' + path, opts);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

// ════════════════════════════════════════════════════════════
//  Bootstrap
// ════════════════════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('start-btn').addEventListener('click', startGame);
  document.getElementById('replay-btn').addEventListener('click', replayGame);
});

async function startGame() {
  const btn = document.getElementById('start-btn');
  btn.textContent = 'Chargement…';
  btn.disabled = true;

  try {
    const state = await api('POST', '/new');
    G.trialId = state.trialId;
    G.state   = state;
    setPhaseActions(state.phase);

    document.getElementById('welcome-screen').classList.add('hidden');
    document.getElementById('game-screen').classList.remove('hidden');
    render(state);
  } catch (e) {
    btn.textContent = 'Commencer une Affaire';
    btn.disabled = false;
    toast('Erreur lors du démarrage : ' + e.message, 'error');
  }
}

async function replayGame() {
  document.getElementById('verdict-overlay').classList.add('hidden');
  document.getElementById('welcome-screen').classList.remove('hidden');
  document.getElementById('game-screen').classList.add('hidden');

  const btn = document.getElementById('start-btn');
  btn.textContent = 'Commencer une Affaire';
  btn.disabled = false;
  G.trialId = null; G.state = null;
  G.selectedWitness = -1; G.confrontSel = [];
}

// ════════════════════════════════════════════════════════════
//  Render principal
// ════════════════════════════════════════════════════════════
function render(state) {
  G.state = state;
  renderHeader(state);
  renderSidebar(state);
  renderPhasePanel(state);
}

// ─── Header ─────────────────────────────────────────────────
function renderHeader(state) {
  // Phase nav
  const nav = document.getElementById('phase-nav');
  nav.innerHTML = PHASE_LABELS.map((label, i) => {
    const cls = i < state.phaseIndex ? 'done'
              : i === state.phaseIndex ? 'active' : '';
    const icon = i < state.phaseIndex ? '✓' : (i + 1);
    return `<div class="phase-step ${cls}">
      <span class="step-num">${icon}</span>
      <span class="step-label">${label}</span>
    </div>`;
  }).join('');

  // Jury bar
  const pct = Math.round(state.juryConviction * 100);
  const fill = document.getElementById('jury-fill');
  fill.style.width = pct + '%';
  // Vert = bon pour la défense (conviction basse), rouge = mauvais
  fill.style.background = pct < 40 ? '#27ae60' : pct < 65 ? '#f39c12' : '#e74c3c';
  document.getElementById('jury-pct').textContent = pct + '%';

  // Score
  document.getElementById('score-val').textContent =
    state.successfulActions + '/' + state.totalActions;
}

// ─── Sidebar ─────────────────────────────────────────────────
function renderSidebar(state) {
  document.getElementById('case-title').textContent = state.caseTitle;

  const badge = document.getElementById('crime-badge');
  badge.textContent = CRIME_FR[state.crimeType] || state.crimeType;
  badge.className = 'crime-badge ' + (state.crimeType || '').toLowerCase();

  document.getElementById('suspect-name').textContent = state.suspectName;
  document.getElementById('case-desc').textContent = state.caseDescription;

  // Events
  const list = document.getElementById('events-list');
  const events = [...(state.events || [])].reverse();
  list.innerHTML = events.length
    ? events.map(e => {
        const cls = e.startsWith('✔') || e.startsWith('⚡') ? 'success'
                  : e.startsWith('✘') ? 'fail' : '';
        return `<div class="event-item ${cls}">${e}</div>`;
      }).join('')
    : '<div class="event-item">Aucun événement.</div>';
}

// ════════════════════════════════════════════════════════════
//  Phase Panel (contenu dynamique)
// ════════════════════════════════════════════════════════════
function renderPhasePanel(state) {
  const panel = document.getElementById('phase-panel');
  panel.innerHTML = '';

  switch (state.phase) {
    case 'OPENING_STATEMENTS':  renderOpening(panel, state);        break;
    case 'PROSECUTION_CASE':    renderProsecution(panel, state);    break;
    case 'DEFENSE_CASE':        renderDefense(panel, state);        break;
    case 'CROSS_EXAMINATION':   renderCrossExam(panel, state);      break;
    case 'CLOSING_ARGUMENTS':   renderClosing(panel, state);        break;
    case 'VERDICT':             triggerVerdict();                   break;
  }
}

// ─── Phase 1 : Ouverture ─────────────────────────────────────
function renderOpening(panel, state) {
  panel.innerHTML = `
    <div class="phase-header">
      <div>
        <h2 class="phase-title">Déclarations d'ouverture</h2>
        <p class="phase-subtitle">Prenez connaissance du dossier avant de commencer.</p>
      </div>
    </div>

    <div>
      <h3 style="font-size:.85rem;color:var(--text-muted);text-transform:uppercase;letter-spacing:.08em;margin-bottom:.75rem;">Témoins</h3>
      <div class="card-grid" id="witness-cards"></div>
    </div>

    <div>
      <h3 style="font-size:.85rem;color:var(--text-muted);text-transform:uppercase;letter-spacing:.08em;margin-bottom:.75rem;">Preuves</h3>
      <div class="card-grid" id="evidence-cards"></div>
    </div>

    <div class="next-bar">
      <button class="btn-primary" id="next-btn">Commencer le procès →</button>
    </div>`;

  renderWitnessCards('#witness-cards', state, false);
  renderEvidenceCards('#evidence-cards', state, false);

  document.getElementById('next-btn').addEventListener('click', advancePhase);
}

// ─── Phase 2 : Accusation ─────────────────────────────────────
function renderProsecution(panel, state) {
  panel.innerHTML = `
    <div class="phase-header">
      <div>
        <h2 class="phase-title">Cas de l'accusation</h2>
        <p class="phase-subtitle">Contestez les preuves douteuses pour influencer le jury.</p>
      </div>
      <div class="actions-left-badge" id="actions-badge">
        ${G.actionsLeft} action${G.actionsLeft > 1 ? 's' : ''} restante${G.actionsLeft > 1 ? 's' : ''}
      </div>
    </div>

    <div class="card-grid" id="evidence-cards"></div>

    <div class="next-bar">
      <button class="btn-outline" id="next-btn">Passer à la défense →</button>
    </div>`;

  renderEvidenceCards('#evidence-cards', state, true);
  document.getElementById('next-btn').addEventListener('click', advancePhase);
}

// ─── Phase 3 : Défense ─────────────────────────────────────
function renderDefense(panel, state) {
  panel.innerHTML = `
    <div class="phase-header">
      <div>
        <h2 class="phase-title">Cas de la défense</h2>
        <p class="phase-subtitle">Sélectionnez un témoin et posez-lui une question pour révéler des contradictions.</p>
      </div>
      <div class="actions-left-badge" id="actions-badge">
        ${G.actionsLeft} action${G.actionsLeft > 1 ? 's' : ''} restante${G.actionsLeft > 1 ? 's' : ''}
      </div>
    </div>

    <div class="card-grid" id="witness-cards"></div>

    <div id="question-area" style="display:none"></div>
    <div id="response-area"></div>

    <div class="next-bar">
      <button class="btn-outline" id="next-btn">Contre-interrogatoire →</button>
    </div>`;

  renderWitnessCards('#witness-cards', state, true);
  document.getElementById('next-btn').addEventListener('click', advancePhase);
}

// ─── Phase 4 : Contre-interrogatoire ──────────────────────────
function renderCrossExam(panel, state) {
  G.confrontSel = [];

  panel.innerHTML = `
    <div class="phase-header">
      <div>
        <h2 class="phase-title">Contre-interrogatoire</h2>
        <p class="phase-subtitle">Sélectionnez deux témoins et confrontez leurs déclarations.</p>
      </div>
      <div class="actions-left-badge" id="actions-badge">
        ${G.actionsLeft} action${G.actionsLeft > 1 ? 's' : ''} restante${G.actionsLeft > 1 ? 's' : ''}
      </div>
    </div>

    <div class="card-grid" id="witness-cards"></div>

    <div class="confront-section" id="confront-form" style="display:none">
      <h4>Confronter ${state.witnesses[0]?.name ?? '—'} et ${state.witnesses[1]?.name ?? '—'}</h4>
      <div class="confront-form">
        <input id="topic-input" type="text" placeholder="Sujet de la confrontation (ex: alibi, présence...)" />
        <button class="btn-success" id="confront-btn">Confronter</button>
      </div>
    </div>

    <div id="response-area"></div>

    <div class="next-bar">
      <button class="btn-outline" id="next-btn">Passer aux plaidoiries →</button>
    </div>`;

  renderWitnessCards('#witness-cards', state, true);
  document.getElementById('next-btn').addEventListener('click', advancePhase);
}

// ─── Phase 5 : Plaidoiries ──────────────────────────────────
function renderClosing(panel, state) {
  panel.innerHTML = `
    <div class="phase-header">
      <div>
        <h2 class="phase-title">Plaidoiries finales</h2>
        <p class="phase-subtitle">Votre dernière chance de convaincre le jury.</p>
      </div>
    </div>

    <div class="plea-section">
      <h3>Votre plaidoirie</h3>
      <p>Rédigez votre plaidoirie finale pour défendre votre client (optionnel).</p>
      <textarea id="plea-input" placeholder="Membres du jury, mon client est innocent car…"></textarea>
      <button class="btn-primary" id="verdict-btn">⚖ Rendre le verdict</button>
    </div>`;

  document.getElementById('verdict-btn').addEventListener('click', async () => {
    const btn = document.getElementById('verdict-btn');
    btn.disabled = true;
    btn.textContent = 'Délibération du jury…';
    try {
      const verdict = await api('POST', `/${G.trialId}/verdict`);
      showVerdict(verdict);
    } catch (e) {
      toast('Erreur : ' + e.message, 'error');
      btn.disabled = false;
      btn.textContent = '⚖ Rendre le verdict';
    }
  });
}

// ════════════════════════════════════════════════════════════
//  Sous-composants
// ════════════════════════════════════════════════════════════

// ─── Cartes Témoins ──────────────────────────────────────────
function renderWitnessCards(selector, state, clickable) {
  const container = document.querySelector(selector);
  if (!container) return;

  container.innerHTML = state.witnesses.map(w => {
    const credColor = w.credibility >= 70 ? '#27ae60' : w.credibility >= 40 ? '#f39c12' : '#e74c3c';
    const stressPct = Math.round(w.stressLevel * 100);
    const stressColor = stressPct < 30 ? '#27ae60' : stressPct < 60 ? '#f39c12' : '#e74c3c';

    return `<div class="witness-card${clickable ? ' clickable' : ''}" data-idx="${w.index}">
      <div class="witness-name">${w.name}</div>
      <div class="witness-job">${w.profession || '—'}</div>
      ${w.initialStatement
        ? `<div class="witness-stmt">« ${w.initialStatement} »</div>`
        : ''}
      <div class="meter-row">
        <span class="meter-label">Crédibilité</span>
        <div class="meter-track"><div class="meter-fill" style="width:${w.credibility}%;background:${credColor}"></div></div>
        <span class="meter-val">${w.credibility}</span>
      </div>
      <div class="meter-row">
        <span class="meter-label">Stress</span>
        <div class="meter-track"><div class="meter-fill" style="width:${stressPct}%;background:${stressColor}"></div></div>
        <span class="meter-val">${stressPct}%</span>
      </div>
    </div>`;
  }).join('');

  if (clickable) {
    container.querySelectorAll('.witness-card').forEach(card => {
      card.addEventListener('click', () => onWitnessClick(parseInt(card.dataset.idx)));
    });
  }
}

// ─── Cartes Preuves ──────────────────────────────────────────
function renderEvidenceCards(selector, state, withContestBtn) {
  const container = document.querySelector(selector);
  if (!container) return;

  container.innerHTML = state.evidences.map(e => {
    const cls = e.contested ? 'evidence-card contested' : e.authentic ? 'evidence-card authentic' : 'evidence-card fake';
    const weightPct = Math.round(e.weight * 100);

    const tags = [];
    if (e.authentic) tags.push('<span class="tag tag-red">Authentique</span>');
    else             tags.push('<span class="tag tag-green">Douteux</span>');
    if (e.contested) tags.push('<span class="tag tag-gold">Contesté</span>');

    const btn = withContestBtn && !e.contested && G.actionsLeft > 0
      ? `<button class="btn-danger" style="font-size:.75rem;padding:.35rem .8rem;" data-idx="${e.index}">Contester</button>`
      : '';

    return `<div class="${cls}">
      <div class="evidence-desc">${e.description}</div>
      <div class="evidence-tags">${tags.join('')}</div>
      <div class="meter-row">
        <span class="meter-label">Force</span>
        <div class="meter-track">
          <div class="meter-fill" style="width:${weightPct}%;background:${e.authentic ? '#e74c3c' : '#27ae60'}"></div>
        </div>
        <span class="meter-val">${weightPct}%</span>
      </div>
      ${btn ? `<div style="margin-top:.65rem">${btn}</div>` : ''}
    </div>`;
  }).join('');

  if (withContestBtn) {
    container.querySelectorAll('[data-idx]').forEach(btn => {
      btn.addEventListener('click', () => onContest(parseInt(btn.dataset.idx)));
    });
  }
}

// ════════════════════════════════════════════════════════════
//  Handlers d'action
// ════════════════════════════════════════════════════════════

// Contester une preuve
async function onContest(idx) {
  if (G.actionsLeft <= 0) return;
  try {
    const result = await api('POST', `/${G.trialId}/contest/${idx}`);
    if (result.success) {
      G.actionsLeft--;
      toast('✔ ' + result.message, 'success');
    } else {
      toast(result.message, 'error');
    }
    updateActionsLeft();
    render(result.gameState);
  } catch (e) {
    toast('Erreur : ' + e.message, 'error');
  }
}

// Cliquer sur un témoin
function onWitnessClick(idx) {
  const phase = G.state?.phase;

  if (phase === 'DEFENSE_CASE') {
    G.selectedWitness = idx;
    highlightWitness(idx);
    showQuestionInput(idx);
    return;
  }

  if (phase === 'CROSS_EXAMINATION') {
    const pos = G.confrontSel.indexOf(idx);
    if (pos !== -1) {
      G.confrontSel.splice(pos, 1);
    } else if (G.confrontSel.length < 2) {
      G.confrontSel.push(idx);
    } else {
      G.confrontSel = [idx];
    }
    highlightConfront();
    updateConfrontForm();
    return;
  }
}

// ─── Défense : afficher la zone de question ──────────────────
function showQuestionInput(witnessIdx) {
  const area = document.getElementById('question-area');
  if (!area) return;
  const w = G.state.witnesses[witnessIdx];

  area.style.display = 'block';
  area.innerHTML = `
    <div class="question-area">
      <h4>Interroger ${w.name}</h4>
      <div class="question-input-row">
        <input id="q-input" type="text" placeholder="Posez votre question…" />
        <button class="btn-success" id="q-btn" ${G.actionsLeft <= 0 ? 'disabled' : ''}>Interroger</button>
      </div>
    </div>`;

  const input = document.getElementById('q-input');
  const btn   = document.getElementById('q-btn');

  input.focus();
  input.addEventListener('keydown', e => { if (e.key === 'Enter') btn.click(); });

  btn.addEventListener('click', async () => {
    const q = input.value.trim();
    if (!q) { toast('Saisissez une question.', 'error'); return; }
    if (G.actionsLeft <= 0) { toast('Plus d\'actions disponibles.', 'error'); return; }
    btn.disabled = true;

    try {
      const result = await api('POST', `/${G.trialId}/question/${witnessIdx}`, { question: q });
      G.actionsLeft--;
      updateActionsLeft();
      showWitnessResponse(result);
      input.value = '';
      render(result.gameState);
      // Re-ouvrir la zone de question pour la même phase
      setTimeout(() => {
        G.selectedWitness = -1;
        document.getElementById('question-area').style.display = 'none';
      }, 50);
    } catch (e) {
      toast('Erreur : ' + e.message, 'error');
      btn.disabled = false;
    }
  });
}

// Afficher la réponse du témoin
function showWitnessResponse(result) {
  const area = document.getElementById('response-area');
  if (!area) return;

  const cls = result.contradictionDetected ? 'response-box contradiction' : 'response-box';
  const alert = result.contradictionDetected
    ? `<div class="contradiction-alert">⚡ ${result.message}</div>`
    : '';

  area.innerHTML = `
    <div class="${cls}">
      <div class="resp-name">
        ${result.contradictionDetected ? '⚡ Contradiction détectée !' : '💬 Réponse du témoin'}
      </div>
      <div class="resp-text">${result.witnessResponse || '—'}</div>
      ${alert}
    </div>`;
}

// ─── Confrontation ────────────────────────────────────────────
function highlightConfront() {
  document.querySelectorAll('.witness-card').forEach(card => {
    const idx = parseInt(card.dataset.idx);
    card.classList.toggle('selected', G.confrontSel.includes(idx));
    // Badge
    card.querySelectorAll('.selected-badge').forEach(b => b.remove());
    if (G.confrontSel.includes(idx)) {
      const badge = document.createElement('span');
      badge.className = 'selected-badge';
      badge.textContent = G.confrontSel.indexOf(idx) === 0 ? 'T1' : 'T2';
      card.appendChild(badge);
    }
  });
}

function highlightWitness(idx) {
  document.querySelectorAll('.witness-card').forEach(card => {
    card.classList.toggle('selected', parseInt(card.dataset.idx) === idx);
  });
}

function updateConfrontForm() {
  const form = document.getElementById('confront-form');
  if (!form) return;
  const ready = G.confrontSel.length === 2;
  form.style.display = ready ? 'block' : 'none';

  if (ready) {
    const w1 = G.state.witnesses[G.confrontSel[0]];
    const w2 = G.state.witnesses[G.confrontSel[1]];
    form.querySelector('h4').textContent = `Confronter ${w1.name} et ${w2.name}`;

    const btn = document.getElementById('confront-btn');
    if (btn) {
      btn.onclick = async () => {
        const topic = document.getElementById('topic-input').value.trim() || 'leur déclaration';
        if (G.actionsLeft <= 0) { toast('Plus d\'actions disponibles.', 'error'); return; }
        btn.disabled = true;

        try {
          const result = await api('POST', `/${G.trialId}/confront`, {
            witness1: G.confrontSel[0],
            witness2: G.confrontSel[1],
            topic
          });
          G.actionsLeft--;
          G.confrontSel = [];
          updateActionsLeft();

          const area = document.getElementById('response-area');
          if (area) {
            const cls = result.contradictionDetected ? 'response-box contradiction' : 'response-box';
            area.innerHTML = `<div class="${cls}">
              <div class="resp-name">${result.contradictionDetected ? '⚡ Contradiction !' : '🔍 Résultat'}</div>
              <div class="resp-text">${result.message}</div>
            </div>`;
          }

          render(result.gameState);
        } catch (e) {
          toast('Erreur : ' + e.message, 'error');
          btn.disabled = false;
        }
      };
    }
  }
}

// ════════════════════════════════════════════════════════════
//  Avancement de phase
// ════════════════════════════════════════════════════════════
async function advancePhase() {
  try {
    const state = await api('POST', `/${G.trialId}/next`);
    G.selectedWitness = -1;
    G.confrontSel = [];
    setPhaseActions(state.phase);
    render(state);
  } catch (e) {
    toast('Erreur : ' + e.message, 'error');
  }
}

function setPhaseActions(phase) {
  G.actionsLeft = PHASE_MAX_ACTIONS[phase] ?? 0;
}

function updateActionsLeft() {
  const badge = document.getElementById('actions-badge');
  if (!badge) return;
  const n = G.actionsLeft;
  badge.textContent = `${n} action${n > 1 ? 's' : ''} restante${n > 1 ? 's' : ''}`;
}

// ════════════════════════════════════════════════════════════
//  Verdict
// ════════════════════════════════════════════════════════════
async function triggerVerdict() {
  try {
    const verdict = await api('POST', `/${G.trialId}/verdict`);
    showVerdict(verdict);
  } catch (e) {
    toast('Erreur verdict : ' + e.message, 'error');
  }
}

function showVerdict(v) {
  document.getElementById('v-suspect').textContent = v.suspectName;

  const resultEl = document.getElementById('v-result');
  if (v.status === 'NOT_GUILTY') {
    resultEl.textContent = '✔ NON COUPABLE';
    resultEl.className = 'verdict-result not-guilty';
  } else {
    resultEl.textContent = '✘ COUPABLE';
    resultEl.className = 'verdict-result guilty';
  }

  // Jurés
  const jurors = document.getElementById('v-jurors');
  jurors.innerHTML = '';
  for (let i = 0; i < v.totalJurors; i++) {
    const dot = document.createElement('div');
    dot.className = 'juror-dot ' + (i < v.guiltyVotes ? 'guilty' : 'innocent');
    jurors.appendChild(dot);
  }
  document.getElementById('v-votes').textContent = v.guiltyVotes + '/' + v.totalJurors;

  // Score
  const scoreFill = document.getElementById('v-score-fill');
  scoreFill.style.width = '0%';
  const scoreColor = v.playerScore >= 70 ? '#27ae60' : v.playerScore >= 40 ? '#f39c12' : '#e74c3c';
  scoreFill.style.background = scoreColor;
  document.getElementById('v-score').textContent = v.playerScore + '/100';
  setTimeout(() => { scoreFill.style.width = v.playerScore + '%'; }, 200);

  // Vérité
  const truth = document.getElementById('v-truth');
  if (v.wasActuallyGuilty) {
    truth.className = 'v-truth guilty-truth';
    truth.textContent = '⚠ Votre client était réellement coupable.';
  } else {
    truth.className = 'v-truth innocent-truth';
    truth.textContent = '✔ Votre client était réellement innocent !';
  }

  document.getElementById('v-explanation').textContent = v.explanation;

  document.getElementById('verdict-overlay').classList.remove('hidden');
}

// ════════════════════════════════════════════════════════════
//  Toast
// ════════════════════════════════════════════════════════════
let toastTimer = null;
function toast(msg, type = '') {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.className = 'toast' + (type ? ' ' + type : '');
  el.classList.remove('hidden');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.add('hidden'), 3500);
}
