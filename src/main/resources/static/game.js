'use strict';

// ════════════════════════════════════════════════════════════
//  État global
// ════════════════════════════════════════════════════════════
const G = {
  trialId:         null,
  state:           null,
  actionsLeft:     0,
  selectedWitness: -1,
  confrontSel:     [],
  lastResponse:    null,   // Dernière réponse du témoin (persistante)
};

const PHASE_MAX_ACTIONS = {
  PROSECUTION_CASE:  3,
  DEFENSE_CASE:      5,
  CROSS_EXAMINATION: 2,
};

const PHASE_LABELS = [
  'Ouverture', 'Accusation', 'Défense', 'Contre-interro.', 'Plaidoirie', 'Verdict'
];

const CRIME_FR = {
  MURDER: 'Meurtre', FRAUD: 'Fraude',
  THEFT: 'Vol', ASSAULT: 'Agression', CORRUPTION: 'Corruption'
};

// Questions suggérées pour la défense
const SUGGESTED_QUESTIONS = [
  'Où étiez-vous au moment des faits ?',
  'Connaissez-vous personnellement l\'accusé(e) ?',
  'Êtes-vous certain(e) de ce que vous avez vu ?',
  'Aviez-vous des raisons de mentir ?',
  'Votre témoignage a-t-il changé depuis la première audition ?',
  'Quelqu\'un vous a-t-il influencé avant de témoigner ?',
  'Pouvez-vous décrire précisément ce que vous avez vu ?',
];

// ════════════════════════════════════════════════════════════
//  API
// ════════════════════════════════════════════════════════════
async function api(method, path, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch('/api/game' + path, opts);
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

// ════════════════════════════════════════════════════════════
//  Bootstrap
// ════════════════════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('start-btn').addEventListener('click', showTutorial);
  document.getElementById('replay-btn').addEventListener('click', replayGame);
  document.getElementById('tuto-skip-btn').addEventListener('click', launchGame);
  document.getElementById('tuto-play-btn').addEventListener('click', launchGame);
});

function showTutorial() {
  document.getElementById('welcome-screen').classList.add('hidden');
  document.getElementById('tutorial-overlay').classList.remove('hidden');
}

async function launchGame() {
  const overlay = document.getElementById('tutorial-overlay');
  overlay.classList.add('hidden');
  const playBtn = document.getElementById('tuto-play-btn');
  playBtn.textContent = 'Chargement…';
  playBtn.disabled = true;
  try {
    const state = await api('POST', '/new');
    G.trialId = state.trialId;
    G.state   = state;
    G.lastResponse = null;
    setPhaseActions(state.phase);
    document.getElementById('game-screen').classList.remove('hidden');
    render(state);
  } catch (e) {
    overlay.classList.remove('hidden');
    playBtn.textContent = 'Commencer le procès !';
    playBtn.disabled = false;
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
  G.trialId = null; G.state = null; G.lastResponse = null;
  G.selectedWitness = -1; G.confrontSel = [];
}

// ════════════════════════════════════════════════════════════
//  Render
// ════════════════════════════════════════════════════════════

/** Render complet : header + sidebar + phase panel. */
function render(state) {
  G.state = state;
  renderHeader(state);
  renderSidebar(state);
  renderPhasePanel(state);
}

/** Render partiel : header + sidebar seulement. Utilisé après une action
 *  pour ne pas perturber la zone de réponse / saisie en cours. */
function softRender(state) {
  G.state = state;
  renderHeader(state);
  renderSidebar(state);
}

// ─── Header ─────────────────────────────────────────────────
function renderHeader(state) {
  const nav = document.getElementById('phase-nav');
  nav.innerHTML = PHASE_LABELS.map((label, i) => {
    const cls  = i < state.phaseIndex ? 'done' : i === state.phaseIndex ? 'active' : '';
    const icon = i < state.phaseIndex ? '✓' : i + 1;
    return `<div class="phase-step ${cls}">
      <span class="step-num">${icon}</span>
      <span class="step-label">${label}</span>
    </div>`;
  }).join('');

  const pct  = Math.round(state.juryConviction * 100);
  const fill = document.getElementById('jury-fill');
  fill.style.width      = pct + '%';
  fill.style.background = pct < 40 ? '#27ae60' : pct < 65 ? '#f39c12' : '#e74c3c';
  document.getElementById('jury-pct').textContent   = pct + '%';
  document.getElementById('score-val').textContent  =
    state.successfulActions + '/' + state.totalActions;
}

// ─── Sidebar ─────────────────────────────────────────────────
function renderSidebar(state) {
  document.getElementById('case-title').textContent = state.caseTitle;

  const badge = document.getElementById('crime-badge');
  badge.textContent = CRIME_FR[state.crimeType] || state.crimeType;
  badge.className   = 'crime-badge ' + (state.crimeType || '').toLowerCase();

  document.getElementById('suspect-name').textContent = state.suspectName;
  document.getElementById('case-desc').textContent    = state.caseDescription;

  const list   = document.getElementById('events-list');
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
//  Phase Panel
// ════════════════════════════════════════════════════════════
function renderPhasePanel(state) {
  const panel = document.getElementById('phase-panel');
  panel.innerHTML = '';
  G.selectedWitness = -1;
  G.confrontSel     = [];
  G.lastResponse    = null;

  switch (state.phase) {
    case 'OPENING_STATEMENTS': renderOpening(panel, state);     break;
    case 'PROSECUTION_CASE':   renderProsecution(panel, state); break;
    case 'DEFENSE_CASE':       renderDefense(panel, state);     break;
    case 'CROSS_EXAMINATION':  renderCrossExam(panel, state);   break;
    case 'CLOSING_ARGUMENTS':  renderClosing(panel, state);     break;
    case 'VERDICT':            triggerVerdict();                break;
  }
}

// ─── Phase 1 : Ouverture ─────────────────────────────────────
function renderOpening(panel, state) {
  panel.innerHTML = `
    <div class="phase-guide guide-blue">
      <span class="guide-icon">📋</span>
      <div>
        <strong>Déclarations d'ouverture</strong>
        <p>Prenez connaissance du dossier : les témoins, les preuves et l'accusé(e). C'est votre seule chance de tout lire avant le début du procès.</p>
      </div>
    </div>

    <div class="section-label">Témoins à interroger</div>
    <div class="card-grid" id="witness-cards"></div>

    <div class="section-label" style="margin-top:1rem">Preuves présentées</div>
    <div class="card-grid" id="evidence-cards"></div>

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
    <div class="phase-guide guide-red">
      <span class="guide-icon">⚔️</span>
      <div>
        <strong>Phase d'accusation — Contestez les preuves douteuses</strong>
        <p>
          Cliquez sur <strong>Contester</strong> sous une preuve qui vous semble fausse ou fabriquée.
          Une preuve <span style="color:#e57373">authentique</span> vous nuit,
          une preuve <span style="color:#66bb6a">douteuse</span> peut être neutralisée.
        </p>
      </div>
    </div>

    <div class="actions-bar">
      <span class="actions-label">Actions restantes :</span>
      <div class="action-dots" id="action-dots"></div>
    </div>

    <div class="card-grid" id="evidence-cards"></div>

    <div class="next-bar">
      <button class="btn-outline" id="next-btn">Passer à la défense →</button>
    </div>`;

  renderActionDots(G.actionsLeft, PHASE_MAX_ACTIONS.PROSECUTION_CASE);
  renderEvidenceCards('#evidence-cards', state, true);
  document.getElementById('next-btn').addEventListener('click', advancePhase);
}

// ─── Phase 3 : Défense ─────────────────────────────────────
function renderDefense(panel, state) {
  panel.innerHTML = `
    <div class="phase-guide guide-green">
      <span class="guide-icon">🛡️</span>
      <div>
        <strong>Défense — Interrogez les témoins</strong>
        <p>
          <strong>Cliquez sur un témoin</strong> pour l'interroger.
          Choisissez une question suggérée ou écrivez la vôtre.
          Cherchez des contradictions dans leurs déclarations !
        </p>
      </div>
    </div>

    <div class="actions-bar">
      <span class="actions-label">Questions restantes :</span>
      <div class="action-dots" id="action-dots"></div>
    </div>

    <div class="card-grid" id="witness-cards"></div>

    <div id="question-box" class="question-box hidden"></div>
    <div id="response-area"></div>

    <div class="next-bar">
      <button class="btn-outline" id="next-btn">Contre-interrogatoire →</button>
    </div>`;

  renderActionDots(G.actionsLeft, PHASE_MAX_ACTIONS.DEFENSE_CASE);
  renderWitnessCards('#witness-cards', state, true);
  document.getElementById('next-btn').addEventListener('click', advancePhase);
}

// ─── Phase 4 : Contre-interrogatoire ──────────────────────────
function renderCrossExam(panel, state) {
  panel.innerHTML = `
    <div class="phase-guide guide-gold">
      <span class="guide-icon">🔍</span>
      <div>
        <strong>Contre-interrogatoire — Confrontez deux témoins</strong>
        <p>
          Cliquez sur <strong>deux témoins</strong> pour les sélectionner (badges T1 et T2).
          Entrez le sujet de la confrontation et cliquez <strong>Confronter</strong>.
        </p>
      </div>
    </div>

    <div class="actions-bar">
      <span class="actions-label">Confrontations restantes :</span>
      <div class="action-dots" id="action-dots"></div>
    </div>

    <div class="card-grid" id="witness-cards"></div>

    <div class="confront-section hidden" id="confront-form">
      <h4 id="confront-title">Sélectionnez deux témoins</h4>
      <div class="confront-form">
        <input id="topic-input" type="text" placeholder="Sujet : alibi, présence sur les lieux, heure des faits…" />
        <button class="btn-success" id="confront-btn">⚡ Confronter</button>
      </div>
    </div>

    <div id="response-area"></div>

    <div class="next-bar">
      <button class="btn-outline" id="next-btn">Passer aux plaidoiries →</button>
    </div>`;

  renderActionDots(G.actionsLeft, PHASE_MAX_ACTIONS.CROSS_EXAMINATION);
  renderWitnessCards('#witness-cards', state, true);
  document.getElementById('next-btn').addEventListener('click', advancePhase);
}

// ─── Phase 5 : Plaidoiries ──────────────────────────────────
function renderClosing(panel, state) {
  panel.innerHTML = `
    <div class="phase-guide guide-gold">
      <span class="guide-icon">🎤</span>
      <div>
        <strong>Plaidoiries finales — Convaincre le jury</strong>
        <p>Rédigez votre plaidoirie finale pour défendre votre client. Ce texte est cosmétique, mais profitez-en pour récapituler vos arguments avant le verdict !</p>
      </div>
    </div>

    <div class="plea-section">
      <h3>Votre plaidoirie</h3>
      <p>Membres du jury, vous avez entendu les témoignages et vu les preuves. Il est temps de délibérer.</p>
      <textarea id="plea-input" placeholder="Membres du jury, mon client est innocent car…"></textarea>
      <button class="btn-primary" id="verdict-btn">⚖ Rendre le verdict</button>
    </div>`;

  document.getElementById('verdict-btn').addEventListener('click', async () => {
    const btn = document.getElementById('verdict-btn');
    btn.disabled = true;
    btn.textContent = 'Le jury délibère…';
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

function renderActionDots(left, max) {
  const el = document.getElementById('action-dots');
  if (!el) return;
  el.innerHTML = '';
  for (let i = 0; i < max; i++) {
    const dot = document.createElement('span');
    dot.className = 'action-dot ' + (i < left ? 'active' : 'used');
    el.appendChild(dot);
  }
}

function renderWitnessCards(selector, state, clickable) {
  const container = document.querySelector(selector);
  if (!container) return;

  container.innerHTML = state.witnesses.map(w => {
    const credPct   = w.credibility;
    const credColor = credPct >= 70 ? '#27ae60' : credPct >= 40 ? '#f39c12' : '#e74c3c';
    const stressPct = Math.round(w.stressLevel * 100);
    const stressCol = stressPct < 30 ? '#27ae60' : stressPct < 60 ? '#f39c12' : '#e74c3c';
    const isSelected = G.confrontSel.includes(w.index) ||
                       G.selectedWitness === w.index;

    return `<div class="witness-card${clickable ? ' clickable' : ''}${isSelected ? ' selected' : ''}"
                 data-idx="${w.index}" title="${clickable ? 'Cliquer pour sélectionner ce témoin' : ''}">
      ${clickable ? `<div class="witness-hint">👆 Cliquer pour interroger</div>` : ''}
      <div class="witness-name">${w.name}</div>
      <div class="witness-job">${w.profession || '—'}</div>
      ${w.initialStatement ? `<div class="witness-stmt">« ${w.initialStatement} »</div>` : ''}
      <div class="meter-row">
        <span class="meter-label">Crédibilité</span>
        <div class="meter-track"><div class="meter-fill" style="width:${credPct}%;background:${credColor}"></div></div>
        <span class="meter-val">${credPct}</span>
      </div>
      <div class="meter-row">
        <span class="meter-label">Stress</span>
        <div class="meter-track"><div class="meter-fill" style="width:${stressPct}%;background:${stressCol}"></div></div>
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

function renderEvidenceCards(selector, state, withContestBtn) {
  const container = document.querySelector(selector);
  if (!container) return;

  container.innerHTML = state.evidences.map(e => {
    const pct  = Math.round(e.weight * 100);
    const cls  = e.contested
      ? 'evidence-card contested'
      : e.authentic ? 'evidence-card authentic' : 'evidence-card fake';

    const tags = [];
    if (e.authentic) tags.push('<span class="tag tag-red">Authentique</span>');
    else             tags.push('<span class="tag tag-green">Douteux ← à contester !</span>');
    if (e.contested) tags.push('<span class="tag tag-gold">✔ Contesté</span>');

    const canContest = withContestBtn && !e.contested && G.actionsLeft > 0;
    const btn = canContest
      ? `<button class="btn-contest" data-idx="${e.index}">✘ Contester cette preuve</button>`
      : e.contested
        ? `<span class="contested-label">✔ Preuve neutralisée</span>`
        : !e.authentic && G.actionsLeft === 0
          ? `<span class="contested-label" style="color:var(--text-muted)">Plus d'actions</span>`
          : '';

    return `<div class="${cls}">
      <div class="evidence-desc">${e.description}</div>
      <div class="evidence-tags">${tags.join('')}</div>
      <div class="meter-row">
        <span class="meter-label">Force</span>
        <div class="meter-track">
          <div class="meter-fill" style="width:${pct}%;background:${e.authentic ? '#e74c3c' : '#27ae60'}"></div>
        </div>
        <span class="meter-val">${pct}%</span>
      </div>
      ${btn ? `<div style="margin-top:.75rem">${btn}</div>` : ''}
    </div>`;
  }).join('');

  if (withContestBtn) {
    container.querySelectorAll('.btn-contest').forEach(btn => {
      btn.addEventListener('click', () => onContest(parseInt(btn.dataset.idx)));
    });
  }
}

// ════════════════════════════════════════════════════════════
//  Handlers
// ════════════════════════════════════════════════════════════

// ─── Contester une preuve ─────────────────────────────────────
async function onContest(idx) {
  if (G.actionsLeft <= 0) return;
  setLoading(true);
  try {
    const result = await api('POST', `/${G.trialId}/contest/${idx}`);
    if (result.success) {
      G.actionsLeft--;
      toast('✔ ' + result.message, 'success');
    } else {
      toast(result.message, 'error');
    }
    softRender(result.gameState);
    renderActionDots(G.actionsLeft, PHASE_MAX_ACTIONS.PROSECUTION_CASE);
    renderEvidenceCards('#evidence-cards', result.gameState, true);
  } catch (e) {
    toast('Erreur : ' + e.message, 'error');
  } finally {
    setLoading(false);
  }
}

// ─── Clic sur un témoin ───────────────────────────────────────
function onWitnessClick(idx) {
  const phase = G.state?.phase;

  if (phase === 'DEFENSE_CASE') {
    G.selectedWitness = idx;
    // Highlight
    document.querySelectorAll('#witness-cards .witness-card').forEach(c => {
      c.classList.toggle('selected', parseInt(c.dataset.idx) === idx);
    });
    openQuestionBox(idx);
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
    // Mise à jour badges
    document.querySelectorAll('#witness-cards .witness-card').forEach(card => {
      const i = parseInt(card.dataset.idx);
      card.classList.toggle('selected', G.confrontSel.includes(i));
      card.querySelectorAll('.selected-badge').forEach(b => b.remove());
      if (G.confrontSel.includes(i)) {
        const badge = document.createElement('span');
        badge.className = 'selected-badge';
        badge.textContent = G.confrontSel.indexOf(i) === 0 ? 'T1' : 'T2';
        card.appendChild(badge);
      }
    });
    updateConfrontForm();
  }
}

// ─── Zone de question (Défense) ───────────────────────────────
function openQuestionBox(witnessIdx) {
  const box = document.getElementById('question-box');
  if (!box) return;
  const w = G.state.witnesses[witnessIdx];

  box.classList.remove('hidden');
  box.innerHTML = `
    <div class="question-box-inner">
      <div class="qbox-title">Interroger <strong>${w.name}</strong></div>

      <div class="quick-questions">
        ${SUGGESTED_QUESTIONS.map(q =>
          `<button class="q-chip" data-q="${q}">${q}</button>`
        ).join('')}
      </div>

      <div class="question-input-row">
        <input id="q-input" type="text" placeholder="Ou écrivez votre propre question…" />
        <button class="btn-success" id="q-btn" ${G.actionsLeft <= 0 ? 'disabled' : ''}>
          Interroger
        </button>
      </div>
      ${G.actionsLeft === 0 ? '<p class="no-actions-msg">Plus de questions disponibles.</p>' : ''}
    </div>`;

  const input = document.getElementById('q-input');
  const qBtn  = document.getElementById('q-btn');

  // Questions suggérées → remplissent l'input
  box.querySelectorAll('.q-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      input.value = chip.dataset.q;
      input.focus();
    });
  });

  input.focus();
  input.addEventListener('keydown', e => { if (e.key === 'Enter') qBtn.click(); });

  qBtn.addEventListener('click', async () => {
    const q = input.value.trim();
    if (!q)               { toast('Écrivez une question.', 'error'); return; }
    if (G.actionsLeft <= 0) { toast('Plus de questions disponibles.', 'error'); return; }

    qBtn.disabled = true;
    qBtn.textContent = '…';
    setLoading(true);

    try {
      const result = await api('POST', `/${G.trialId}/question/${witnessIdx}`, { question: q });
      G.actionsLeft--;
      G.lastResponse = result;

      softRender(result.gameState);
      renderActionDots(G.actionsLeft, PHASE_MAX_ACTIONS.DEFENSE_CASE);

      // Réafficher la réponse
      showWitnessResponse(result);

      // Remettre l'input à zéro et ré-activer si actions restantes
      input.value  = '';
      input.focus();
      qBtn.textContent = 'Interroger';
      qBtn.disabled    = G.actionsLeft <= 0;

      // Mettre à jour le message "plus d'actions"
      const noMsg = box.querySelector('.no-actions-msg');
      if (G.actionsLeft === 0) {
        if (!noMsg) {
          const p = document.createElement('p');
          p.className = 'no-actions-msg';
          p.textContent = 'Plus de questions disponibles.';
          box.querySelector('.question-box-inner').appendChild(p);
        }
      } else if (noMsg) {
        noMsg.remove();
      }

      // Highlight witness cards (état mis à jour)
      document.querySelectorAll('#witness-cards .witness-card').forEach(c => {
        c.classList.toggle('selected', parseInt(c.dataset.idx) === witnessIdx);
      });
    } catch (e) {
      toast('Erreur : ' + e.message, 'error');
      qBtn.textContent = 'Interroger';
      qBtn.disabled    = false;
    } finally {
      setLoading(false);
    }
  });
}

function showWitnessResponse(result) {
  const area = document.getElementById('response-area');
  if (!area) return;
  const cls  = result.contradictionDetected ? 'response-box contradiction' : 'response-box';
  area.innerHTML = `
    <div class="${cls}">
      <div class="resp-name">
        ${result.contradictionDetected ? '⚡ Contradiction détectée !' : '💬 Réponse du témoin'}
      </div>
      <div class="resp-text">${result.witnessResponse || '—'}</div>
      ${result.contradictionDetected
        ? `<div class="contradiction-alert">⚡ ${result.message}</div>` : ''}
    </div>`;
}

// ─── Confrontation ────────────────────────────────────────────
function updateConfrontForm() {
  const form  = document.getElementById('confront-form');
  const title = document.getElementById('confront-title');
  if (!form) return;

  const ready = G.confrontSel.length === 2;
  form.classList.toggle('hidden', !ready);

  if (ready) {
    const w1 = G.state.witnesses[G.confrontSel[0]];
    const w2 = G.state.witnesses[G.confrontSel[1]];
    title.textContent = `Confronter ${w1.name} et ${w2.name}`;

    const btn = document.getElementById('confront-btn');
    if (btn) {
      btn.onclick = async () => {
        if (G.actionsLeft <= 0) { toast('Plus de confrontations disponibles.', 'error'); return; }
        const topic = (document.getElementById('topic-input')?.value.trim()) || 'leur déclaration';
        btn.disabled = true;
        setLoading(true);
        try {
          const result = await api('POST', `/${G.trialId}/confront`, {
            witness1: G.confrontSel[0],
            witness2: G.confrontSel[1],
            topic
          });
          G.actionsLeft--;
          G.confrontSel = [];
          softRender(result.gameState);
          renderActionDots(G.actionsLeft, PHASE_MAX_ACTIONS.CROSS_EXAMINATION);

          const area = document.getElementById('response-area');
          if (area) {
            const cls = result.contradictionDetected ? 'response-box contradiction' : 'response-box';
            area.innerHTML = `<div class="${cls}">
              <div class="resp-name">${result.contradictionDetected ? '⚡ Contradiction !' : '🔍 Résultat'}</div>
              <div class="resp-text">${result.message}</div>
            </div>`;
          }

          // Désélectionner les témoins
          document.querySelectorAll('#witness-cards .witness-card').forEach(c => {
            c.classList.remove('selected');
            c.querySelectorAll('.selected-badge').forEach(b => b.remove());
          });
          form.classList.add('hidden');
        } catch (e) {
          toast('Erreur : ' + e.message, 'error');
          btn.disabled = false;
        } finally {
          setLoading(false);
        }
      };
    }
  }
}

// ════════════════════════════════════════════════════════════
//  Avancement de phase
// ════════════════════════════════════════════════════════════
async function advancePhase() {
  setLoading(true);
  try {
    const state = await api('POST', `/${G.trialId}/next`);
    G.selectedWitness = -1;
    G.confrontSel     = [];
    G.lastResponse    = null;
    setPhaseActions(state.phase);
    render(state);
  } catch (e) {
    toast('Erreur : ' + e.message, 'error');
  } finally {
    setLoading(false);
  }
}

function setPhaseActions(phase) {
  G.actionsLeft = PHASE_MAX_ACTIONS[phase] ?? 0;
}

// ════════════════════════════════════════════════════════════
//  Verdict
// ════════════════════════════════════════════════════════════
async function triggerVerdict() {
  setLoading(true);
  try {
    const verdict = await api('POST', `/${G.trialId}/verdict`);
    showVerdict(verdict);
  } catch (e) {
    toast('Erreur verdict : ' + e.message, 'error');
  } finally {
    setLoading(false);
  }
}

function showVerdict(v) {
  document.getElementById('v-suspect').textContent = v.suspectName;

  const resultEl = document.getElementById('v-result');
  if (v.status === 'NOT_GUILTY') {
    resultEl.textContent = '✔ NON COUPABLE';
    resultEl.className   = 'verdict-result not-guilty';
  } else {
    resultEl.textContent = '✘ COUPABLE';
    resultEl.className   = 'verdict-result guilty';
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
  const fill  = document.getElementById('v-score-fill');
  const color = v.playerScore >= 70 ? '#27ae60' : v.playerScore >= 40 ? '#f39c12' : '#e74c3c';
  fill.style.width      = '0%';
  fill.style.background = color;
  document.getElementById('v-score').textContent = v.playerScore + '/100';
  setTimeout(() => { fill.style.width = v.playerScore + '%'; }, 200);

  // Vérité
  const truth = document.getElementById('v-truth');
  truth.className   = v.wasActuallyGuilty ? 'v-truth guilty-truth' : 'v-truth innocent-truth';
  truth.textContent = v.wasActuallyGuilty
    ? '⚠ Votre client était réellement coupable.'
    : '✔ Votre client était réellement innocent !';

  document.getElementById('v-explanation').textContent = v.explanation;
  document.getElementById('verdict-overlay').classList.remove('hidden');
}

// ════════════════════════════════════════════════════════════
//  Utilitaires
// ════════════════════════════════════════════════════════════
function setLoading(on) {
  document.body.style.cursor = on ? 'wait' : '';
}

let toastTimer = null;
function toast(msg, type = '') {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.className   = 'toast' + (type ? ' ' + type : '');
  el.classList.remove('hidden');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.add('hidden'), 3500);
}
