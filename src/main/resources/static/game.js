'use strict';

// ════════════════════════════════════════════════════════════
//  État global
// ════════════════════════════════════════════════════════════
const G = {
  trialId:             null,
  state:               null,
  actionsLeft:         0,
  selectedWitness:     -1,
  confrontSel:         [],
  lastResponse:        null,
  selectedTopic:       null,
  questionedWitnesses: new Set(),
  clientConfidence:    50,
  closingTimerInterval: null,
};

// Statistiques de la session en cours
const SESSION = { wins: 0, losses: 0 };

// Drapeau pour la confirmation de passage de phase
let _advanceConfirmPending = false;

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

// Grades et libellés
const GRADE_COLORS  = { S:'#ffd700', A:'#27ae60', B:'#3498db', C:'#f39c12', D:'#e67e22', F:'#e74c3c' };
const GRADE_LABELS  = {
  S: 'Légendaire — on parlera encore de ce procès',
  A: 'Brillant — vous étiez au-dessus',
  B: 'Solide — sans éclat, mais efficace',
  C: 'Passable — vous auriez pu faire mieux',
  D: 'Insuffisant — le jury n\'a pas été convaincu',
  F: 'Désastre — votre client vous en voudra longtemps',
};

// Stratégies de plaidoirie finale
const STRATEGIES = [
  { id:'evidence',  icon:'📋', name:'Attaquer les Preuves',
    desc:'Remettez en cause chaque preuve de l\'accusation. Efficace si vous avez bien travaillé en amont.',
    effect:'Renforce vos contestations précédentes' },
  { id:'character', icon:'💛', name:'Humaniser l\'Accusé(e)',
    desc:'Montrez le côté humain de votre client. Faites appel à l\'empathie des jurés.',
    effect:'Réduit le biais émotionnel du jury' },
  { id:'doubt',     icon:'⚖',  name:'Le Doute Raisonnable',
    desc:'L\'argument classique : sans certitude absolue, l\'innocence prime. Solide et éprouvé.',
    effect:'Fiable dans toutes les situations' },
  { id:'dramatic',  icon:'⚡', name:'Coup de Théâtre',
    desc:'Une révélation de dernière minute. Risqué mais potentiellement décisif. Le juré n°4 se réveille.',
    effect:'Effet surprise — résultat imprévisible' },
];

// Profils des jurés et personnalités des témoins
const JUROR_PROFILES = {
  SKEPTICAL: { icon: '🧐', label: 'Sceptique' },
  EMOTIONAL: { icon: '❤️', label: 'Émotionnel' },
  LOGICAL:   { icon: '🧠', label: 'Logique' },
  BIASED:    { icon: '⚖️', label: 'Partial' },
};

const PERSONALITY_LABELS = {
  NERVOUS:     { icon: '😰', label: 'Nerveux/se' },
  LIAR:        { icon: '🤥', label: 'Menteur/se' },
  CONFIDENT:   { icon: '😤', label: 'Sûr(e) de lui' },
  COOPERATIVE: { icon: '🤝', label: 'Coopératif/ve' },
};

// Événements dramatiques aléatoires entre phases
const PHASE_EVENTS = [
  { icon: '📰', type: 'bad',     title: 'Fuite dans la presse',
    desc: 'Un tabloïd a publié ce matin un portrait peu flatteur de votre client. L\'ambiance dans la salle est électrique.' },
  { icon: '🕵️', type: 'good',    title: 'Document surprise',
    desc: 'Votre assistante a exhumé une vieille condamnation du témoin-clé de l\'accusation. L\'information n\'a pas encore transpiré.' },
  { icon: '😰', type: 'good',    title: 'Témoin en larmes',
    desc: 'Pendant la suspension, un témoin de l\'accusation s\'est effondré dans le couloir. Deux jurés l\'ont vu.' },
  { icon: '🎙️', type: 'bad',     title: 'Déclaration maladroite',
    desc: 'Votre client a parlé aux journalistes devant le palais. Le juge en a été informé. Ça ne va pas arranger les choses.' },
  { icon: '⚡',  type: 'neutral', title: 'Pli urgent',
    desc: 'Un huissier vient d\'entrer avec une enveloppe scellée pour le procureur. Celui-ci blêmit en la lisant. Personne ne sait ce qu\'elle contient.' },
  { icon: '🔒', type: 'good',    title: 'Pièce égarée',
    desc: 'Le greffe signale qu\'une pièce à conviction a temporairement été introuvable. L\'accusation doit s\'en expliquer.' },
  { icon: '👀', type: 'neutral', title: 'Le regard du juré n°4',
    desc: 'Pendant toute la pause, le juré n°4 vous a observé sans ciller. Impossible à cerner — suspicion ou fascination ?' },
  { icon: '💌', type: 'neutral', title: 'Message anonyme',
    desc: 'Un mot glissé sous la porte de votre cabinet : « La vérité n\'est pas là où vous la cherchez. » Signé : personne.' },
];

// Système de sons (Web Audio API — aucune dépendance)
const Sounds = (() => {
  let ctx = null;
  const ensure = () => {
    if (!ctx) ctx = new (window.AudioContext || window.webkitAudioContext)();
    if (ctx.state === 'suspended') ctx.resume();
  };
  const tone = (freq, dur, type = 'sine', vol = 0.08) => {
    try {
      ensure();
      const osc = ctx.createOscillator();
      const g = ctx.createGain();
      osc.connect(g); g.connect(ctx.destination);
      osc.type = type; osc.frequency.value = freq;
      g.gain.setValueAtTime(vol, ctx.currentTime);
      g.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + dur);
      osc.start(); osc.stop(ctx.currentTime + dur);
    } catch (_) {}
  };
  return {
    gavel()         { tone(140, 0.12, 'sawtooth', 0.14); setTimeout(() => tone(90, 0.22, 'sawtooth', 0.09), 85); },
    success()       { tone(523, 0.1, 'sine', 0.07); setTimeout(() => tone(784, 0.28, 'sine', 0.07), 110); },
    contradiction() { tone(220, 0.08, 'square', 0.06); setTimeout(() => tone(880, 0.38, 'sine', 0.07), 95); },
    fail()          { tone(280, 0.08, 'sawtooth', 0.06); setTimeout(() => tone(200, 0.3, 'sawtooth', 0.07), 90); },
    verdictWin()    { [523,659,784,1047].forEach((f,i) => setTimeout(() => tone(f, 0.4, 'sine', 0.07), i*150)); },
    verdictLose()   { [300,240,180].forEach((f,i) => setTimeout(() => tone(f, 0.45, 'sawtooth', 0.08), i*180)); },
  };
})();

// Sujets de confrontation prédéfinis
const CONFRONT_TOPICS = [
  'Alibi', 'Heure des faits', 'Présence sur les lieux',
  'Relation avec la victime', 'Déclaration initiale', 'Témoignage contradictoire',
];

// Questions suggérées par catégorie (alibi, crédibilité, observation, motif, timeline, confrontation)
const SUGGESTED_QUESTIONS = {
  alibi: [
    'Où étiez-vous exactement au moment des faits ?',
    'Pouvez-vous prouver où vous vous trouviez ce soir-là ?',
    'Avez-vous un alibi vérifiable pour cette période ?',
  ],
  credibility: [
    'Votre témoignage a-t-il changé depuis votre première déposition ?',
    'Quelqu\'un vous a-t-il influencé avant de témoigner ?',
    'Aviez-vous des raisons personnelles de mentir ici ?',
  ],
  observation: [
    'Êtes-vous certain(e) de ce que vous avez vu dans ces conditions ?',
    'Pouvez-vous décrire précisément ce que vous avez observé ?',
    'La lumière et la distance vous permettaient-ils vraiment de voir ?',
  ],
  motive: [
    'Pourquoi témoignez-vous contre l\'accusé(e) ?',
    'Avez-vous un intérêt personnel dans le résultat de ce procès ?',
    'Quelle est votre relation réelle avec la victime ?',
  ],
  timeline: [
    'À quelle heure précise avez-vous vu cela ?',
    'Combien de temps s\'est-il écoulé entre les faits et votre appel ?',
    'Étiez-vous présent(e) depuis le début ou arrivé(e) après ?',
  ],
};

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
  // Initialiser l'AudioContext au premier geste utilisateur (contrainte navigateur)
  document.addEventListener('click', () => Sounds.gavel && true, { once: true });
});

// Raccourcis clavier : touches 1-9 pour sélectionner témoins/preuves
document.addEventListener('keydown', e => {
  if (!G.state) return;
  // Ignorer si on tape dans un input/textarea
  if (['INPUT','TEXTAREA'].includes(document.activeElement?.tagName)) return;
  const num = parseInt(e.key);
  if (isNaN(num) || num < 1 || num > 9) return;
  const idx = num - 1;
  const phase = G.state.phase;
  if (phase === 'PROSECUTION_CASE') {
    if (idx < G.state.evidences.length && G.actionsLeft > 0
        && !G.state.evidences[idx].contested) {
      onContest(idx);
    }
  } else if (phase === 'DEFENSE_CASE' || phase === 'CROSS_EXAMINATION') {
    if (idx < G.state.witnesses.length) {
      onWitnessClick(idx);
    }
  }
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
  btn.textContent = 'Prendre une nouvelle affaire';
  btn.disabled = false;
  if (G.closingTimerInterval) { clearInterval(G.closingTimerInterval); G.closingTimerInterval = null; }
  G.trialId             = null;
  G.state               = null;
  G.lastResponse        = null;
  G.selectedWitness     = -1;
  G.confrontSel         = [];
  G.selectedTopic       = null;
  G.questionedWitnesses = new Set();
  G.clientConfidence    = 50;
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

  renderClientConfidence();
  renderJuryPanel(state);

  const list   = document.getElementById('events-list');
  const events = [...(state.events || [])].reverse();
  list.innerHTML = events.length
    ? events.map(e => {
        const cls = e.startsWith('✔') || e.startsWith('⚡') ? 'success'
                  : e.startsWith('✘') ? 'fail' : '';
        return `<div class="event-item ${cls}">${e}</div>`;
      }).join('')
    : '<div class="event-item">Pas encore d\'événement…</div>';
}

function renderClientConfidence() {
  const panel = document.getElementById('confidence-panel');
  if (!panel) return;
  const c = G.clientConfidence;
  const color = c >= 60 ? 'var(--green)' : c >= 35 ? '#f39c12' : 'var(--red)';
  const emoji = c >= 60 ? '😌' : c >= 35 ? '😟' : '😰';
  panel.innerHTML = `
    <div class="confidence-header">
      <span class="confidence-label-text">${emoji} Confiance de votre client</span>
      <span class="confidence-pct">${c}%</span>
    </div>
    <div class="confidence-track">
      <div class="confidence-fill" style="width:${c}%;background:${color}"></div>
    </div>`;
}

function renderJuryPanel(state) {
  const panel = document.getElementById('jury-panel');
  if (!panel || !state.juryMembers?.length) return;

  const rows = state.juryMembers.map(j => {
    const pct    = Math.round(j.convictionLevel * 100);
    const color  = pct < 40 ? 'var(--green)' : pct < 60 ? '#f39c12' : 'var(--red)';
    const cls    = pct < 40 ? 'green' : pct < 60 ? 'orange' : 'red';
    const prof   = JUROR_PROFILES[j.profile] || { icon: '👤', label: '' };
    const short  = j.name?.split(' ')[0] ?? `Juré ${j.index + 1}`;
    return `<div class="juror-row" id="juror-row-${j.index}">
      <span class="juror-icon" title="${prof.label}">${prof.icon}</span>
      <span class="juror-name">${short}</span>
      <div class="juror-mini-bar">
        <div class="juror-mini-fill" style="width:${pct}%;background:${color}"></div>
      </div>
      <span class="juror-pct ${cls}">${pct}%</span>
    </div>`;
  }).join('');

  panel.innerHTML = `
    <h3 class="jury-panel-title">🎯 Le jury</h3>
    <div class="jury-rows">${rows}</div>`;
}

// ════════════════════════════════════════════════════════════
//  Phase Panel
// ════════════════════════════════════════════════════════════
function renderPhasePanel(state) {
  const panel = document.getElementById('phase-panel');
  panel.innerHTML = '';
  G.selectedWitness      = -1;
  G.confrontSel          = [];
  G.lastResponse         = null;
  G.selectedTopic        = null;
  _advanceConfirmPending = false;
  if (G.closingTimerInterval) { clearInterval(G.closingTimerInterval); G.closingTimerInterval = null; }

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
        <strong>Le dossier est ouvert — prenez le temps de tout lire</strong>
        <p>Témoins, preuves, accusé(e). C'est votre seule fenêtre avant que les débats ne commencent. Chaque détail peut faire la différence.</p>
      </div>
    </div>

    <div class="section-label">Les témoins présents</div>
    <div class="card-grid" id="witness-cards"></div>

    <div class="section-label" style="margin-top:1rem">Le dossier de l'accusation</div>
    <div class="card-grid" id="evidence-cards"></div>

    <div class="next-bar">
      <button class="btn-primary" id="next-btn">Que le procès commence →</button>
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
        <strong>L'accusation présente ses arguments — c'est à vous de les démolir</strong>
        <p>
          Certaines de ces preuves sont peut-être fabriquées. Contestez celles qui vous semblent douteuses avant qu'elles ne convainquent le jury.
          Les preuves <strong>lourdes</strong> méritent une attention particulière.
        </p>
      </div>
    </div>

    <div class="actions-bar">
      <span class="actions-label">Il vous reste :</span>
      <div class="action-dots" id="action-dots"></div>
    </div>

    <div class="card-grid" id="evidence-cards"></div>

    <div class="next-bar">
      <button class="btn-outline" id="next-btn">La défense prend la parole →</button>
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
        <strong>C'est votre tour — interrogez vos témoins</strong>
        <p>
          Cliquez sur un témoin pour l'interroger. Un aveu, une hésitation, une contradiction —
          ça peut tout changer. Les témoins nerveux sous pression finissent toujours par se trahir.
        </p>
      </div>
    </div>

    <div class="actions-bar">
      <span class="actions-label">Il vous reste :</span>
      <div class="action-dots" id="action-dots"></div>
    </div>

    <div class="card-grid" id="witness-cards"></div>

    <div id="question-box" class="question-box hidden"></div>
    <div id="response-area"></div>

    <div class="next-bar">
      <button class="btn-outline" id="next-btn">Passer au contre-interrogatoire →</button>
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
        <strong>Les histoires qui ne concordent pas — mettez-les en contradiction</strong>
        <p>
          Sélectionnez <strong>deux témoins</strong> (badges T1/T2), choisissez le sujet sur lequel leurs versions divergent,
          puis confrontez-les. Quand les jurés voient deux témoins se contredire, ça laisse des traces.
        </p>
      </div>
    </div>

    <div class="actions-bar">
      <span class="actions-label">Confrontations restantes :</span>
      <div class="action-dots" id="action-dots"></div>
    </div>

    <div class="card-grid" id="witness-cards"></div>

    <div class="confront-section hidden" id="confront-form">
      <h4 id="confront-title">Confrontation</h4>
      <div class="qcat-label" style="margin-bottom:.5rem">Choisissez un sujet de confrontation</div>
      <div class="quick-questions" id="confront-topic-chips">
        ${CONFRONT_TOPICS.map(t => `<button class="q-chip confront-chip" data-topic="${t}">${t}</button>`).join('')}
      </div>
      <div style="margin-top:.85rem;display:flex;align-items:center;gap:.75rem">
        <button class="btn-success" id="confront-btn" disabled>⚡ Confronter</button>
        <span class="confront-topic-label" id="topic-selected-label"></span>
      </div>
    </div>

    <div id="response-area"></div>

    <div class="next-bar">
      <button class="btn-outline" id="next-btn">Passer aux plaidoiries →</button>
    </div>`;

  renderActionDots(G.actionsLeft, PHASE_MAX_ACTIONS.CROSS_EXAMINATION);
  renderWitnessCards('#witness-cards', state, true);
  document.getElementById('next-btn').addEventListener('click', advancePhase);

  // Handlers chips de sujet
  panel.querySelectorAll('.confront-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      G.selectedTopic = chip.dataset.topic;
      panel.querySelectorAll('.confront-chip').forEach(c => c.classList.remove('selected'));
      chip.classList.add('selected');
      const label = document.getElementById('topic-selected-label');
      if (label) label.textContent = `Sujet : « ${G.selectedTopic} »`;
      const btn = document.getElementById('confront-btn');
      if (btn && G.confrontSel.length === 2) btn.disabled = false;
    });
  });
}

// ─── Phase 5 : Plaidoiries ──────────────────────────────────
function renderClosing(panel, state) {
  const juryMembers   = state.juryMembers || [];
  const guiltyCount   = juryMembers.filter(j => j.convictionLevel > 0.5).length;
  const total         = juryMembers.length || 6;
  const toConvince    = guiltyCount;
  const juryStatusTxt = toConvince === 0
    ? '✅ Le jury penche pour l\'acquittement — ne gâchez pas ça.'
    : toConvince >= total
      ? '⚠ Le jury est unanimement contre vous — il vous faut un miracle.'
      : `⚠ ${toConvince} juré${toConvince > 1 ? 's' : ''} sur ${total} pench${toConvince > 1 ? 'ent' : 'e'} encore pour la culpabilité.`;

  panel.innerHTML = `
    <div class="judge-message">
      <span class="judge-icon">👨‍⚖️</span>
      <div>
        <strong class="judge-name">Le Président du tribunal</strong>
        <p class="judge-text">« Maître, la parole est à vous pour votre plaidoirie finale. Le jury vous écoute. Prenez le temps qu'il vous faut. »</p>
      </div>
    </div>

    <div class="closing-status-bar">
      <span class="closing-jury-info">${juryStatusTxt}</span>
      <div class="closing-timer-wrap">
        <div class="closing-timer-track">
          <div class="closing-timer-fill" id="closing-timer-fill" style="width:100%"></div>
        </div>
        <span class="closing-timer-label"><span class="closing-timer-val" id="closing-timer-val">60</span>s</span>
      </div>
    </div>

    <div class="section-label">Choisissez votre ligne de défense</div>
    <div class="strategy-grid" id="strategy-grid">
      ${STRATEGIES.map(s => `
        <div class="strategy-card" data-id="${s.id}">
          <div class="strategy-icon">${s.icon}</div>
          <div class="strategy-name">${s.name}</div>
          <div class="strategy-desc">${s.desc}</div>
          <div class="strategy-effect">→ ${s.effect}</div>
        </div>`).join('')}
    </div>

    <div class="chosen-strategy hidden" id="chosen-block">
      <div class="chosen-text" id="chosen-text"></div>
      <button class="btn-primary" id="verdict-btn">⚖ Prononcer et demander le verdict</button>
    </div>`;

  // Timer cosmétique 60 secondes
  let seconds = 60;
  const timerValEl  = document.getElementById('closing-timer-val');
  const timerFillEl = document.getElementById('closing-timer-fill');
  G.closingTimerInterval = setInterval(() => {
    seconds--;
    if (timerValEl)  timerValEl.textContent = Math.max(0, seconds);
    if (timerFillEl) timerFillEl.style.width = Math.max(0, (seconds / 60) * 100) + '%';
    if (seconds <= 0) { clearInterval(G.closingTimerInterval); G.closingTimerInterval = null; }
  }, 1000);

  panel.querySelectorAll('.strategy-card').forEach(card => {
    card.addEventListener('click', () => {
      panel.querySelectorAll('.strategy-card').forEach(c => c.classList.remove('selected'));
      card.classList.add('selected');
      const s = STRATEGIES.find(x => x.id === card.dataset.id);
      document.getElementById('chosen-text').textContent =
        `Stratégie choisie : « ${s.name} » — ${s.desc}`;
      document.getElementById('chosen-block').classList.remove('hidden');

      const btn = document.getElementById('verdict-btn');
      btn.onclick = async () => {
        btn.disabled = true;
        btn.textContent = '⌛ Le jury délibère…';
        if (G.closingTimerInterval) { clearInterval(G.closingTimerInterval); G.closingTimerInterval = null; }
        try {
          const verdict = await api('POST', `/${G.trialId}/verdict`);
          showVerdict(verdict);
        } catch (e) {
          toast('Erreur : ' + e.message, 'error');
          btn.disabled = false;
          btn.textContent = '⚖ Prononcer et demander le verdict';
        }
      };
    });
  });
}

// ════════════════════════════════════════════════════════════
//  Sous-composants
// ════════════════════════════════════════════════════════════

function witnessEmoji(w, revealed) {
  if (!revealed) return '🔮';
  const p = PERSONALITY_LABELS[w.personality];
  if (p) return p.icon;
  const stressPct = Math.round(w.stressLevel * 100);
  if (stressPct > 70) return '😰';
  if (w.credibility < 30) return '🤥';
  if (w.credibility >= 75) return '🧑‍⚖️';
  return '👤';
}

// Effet machine à écrire
function typewriter(el, text, speed = 16) {
  let i = 0; el.textContent = '';
  const iv = setInterval(() => {
    el.textContent += text[i++];
    if (i >= text.length) clearInterval(iv);
  }, speed);
}

// Flash d'animation sur une carte
function flashCard(el, type) {
  if (!el) return;
  el.classList.add(`flash-${type}`);
  setTimeout(() => el.classList.remove(`flash-${type}`), 700);
}

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

  container.innerHTML = state.witnesses.map((w, listIdx) => {
    const credPct    = w.credibility;
    const credColor  = credPct >= 70 ? '#27ae60' : credPct >= 40 ? '#f39c12' : '#e74c3c';
    const stressPct  = Math.round(w.stressLevel * 100);
    const stressCol  = stressPct < 30 ? '#27ae60' : stressPct < 60 ? '#f39c12' : '#e74c3c';
    const isSelected = G.confrontSel.includes(w.index) || G.selectedWitness === w.index;
    const revealed   = G.questionedWitnesses.has(w.index);
    const emoji      = witnessEmoji(w, revealed);
    const shortcut   = clickable ? `<span class="kbd-hint">${listIdx + 1}</span>` : '';

    const personalityBadge = revealed && w.personality
      ? `<span class="personality-badge">${PERSONALITY_LABELS[w.personality]?.icon ?? ''} ${PERSONALITY_LABELS[w.personality]?.label ?? w.personality}</span>`
      : `<span class="personality-badge unknown">🔮 Profil non révélé</span>`;

    return `<div class="witness-card${clickable ? ' clickable' : ''}${isSelected ? ' selected' : ''}"
                 data-idx="${w.index}" title="${clickable ? 'Cliquer pour interroger ce témoin' : ''}">
      ${shortcut}
      ${clickable ? `<div class="witness-hint">👆 Cliquer pour interroger</div>` : ''}
      <div class="witness-name">${emoji} ${w.name}</div>
      <div class="witness-job">${w.profession || '—'}</div>
      ${personalityBadge}
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

  container.innerHTML = state.evidences.map((e, listIdx) => {
    const pct  = Math.round(e.weight * 100);
    const cls = e.contested
      ? 'evidence-card contested'
      : withContestBtn
        ? 'evidence-card'
        : e.authentic ? 'evidence-card authentic' : 'evidence-card fake';

    const tags = [];
    if (e.contested) {
      tags.push('<span class="tag tag-gold">✔ Neutralisée</span>');
    } else if (withContestBtn) {
      // Phase accusation : on ne révèle pas si la preuve est vraie ou fausse
      tags.push('<span class="tag tag-muted">Non vérifiée</span>');
      if (pct >= 75) tags.push('<span class="tag tag-danger">⚠ Lourde</span>');
    } else {
      // Phase ouverture : on peut tout lire
      if (e.authentic && pct >= 75) tags.push('<span class="tag tag-danger">🚨 CRITIQUE</span>');
      if (e.authentic) tags.push('<span class="tag tag-red">Authentique</span>');
      else tags.push('<span class="tag tag-green">Douteuse</span>');
    }
    const shortcut = withContestBtn && !e.contested ? `<span class="kbd-hint">${listIdx + 1}</span>` : '';

    const canContest = withContestBtn && !e.contested && G.actionsLeft > 0;
    const btn = canContest
      ? `<button class="btn-contest" data-idx="${e.index}">✘ Contester cette preuve</button>`
      : e.contested
        ? `<span class="contested-label">✔ Preuve neutralisée</span>`
        : !e.authentic && G.actionsLeft === 0
          ? `<span class="contested-label" style="color:var(--text-muted)">Plus d'actions</span>`
          : '';

    return `<div class="${cls}">
      ${shortcut}
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
      updateClientConfidence(+8);
      Sounds.success();
      toast('✔ ' + result.message, 'success');
      const card = document.querySelector(`#evidence-cards .evidence-card:nth-child(${idx + 1})`);
      flashCard(card, 'success');
    } else {
      updateClientConfidence(-10);
      Sounds.fail();
      toast(result.message, 'error');
      const card = document.querySelector(`#evidence-cards .evidence-card:nth-child(${idx + 1})`);
      flashCard(card, 'fail');
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

function updateClientConfidence(delta) {
  G.clientConfidence = Math.max(0, Math.min(100, G.clientConfidence + delta));
  renderClientConfidence();
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

  rebuildQuestionBox(box, witnessIdx, w);
}

function rebuildQuestionBox(box, witnessIdx, w) {
  const disabled = G.actionsLeft <= 0;
  box.classList.remove('hidden');

  const makeChips = (questions) => questions.map(q =>
    `<button class="q-chip${disabled ? ' q-chip-disabled' : ''}" data-q="${q}" ${disabled ? 'disabled' : ''}>${q}</button>`
  ).join('');

  box.innerHTML = `
    <div class="question-box-inner">
      <div class="qbox-title">Interroger <strong>${w.name}</strong>
        <span class="qbox-actions-left${disabled ? ' exhausted' : ''}">${G.actionsLeft} question${G.actionsLeft !== 1 ? 's' : ''} restante${G.actionsLeft !== 1 ? 's' : ''}</span>
      </div>
      ${disabled ? '<p class="no-actions-msg">Plus de questions disponibles pour ce témoin.</p>' : ''}
      <div class="quick-questions-cats">
        <div class="qcat-label">Alibi &amp; présence</div>
        <div class="quick-questions">${makeChips(SUGGESTED_QUESTIONS.alibi)}</div>
        <div class="qcat-label">Crédibilité</div>
        <div class="quick-questions">${makeChips(SUGGESTED_QUESTIONS.credibility)}</div>
        <div class="qcat-label">Observations &amp; faits</div>
        <div class="quick-questions">${makeChips(SUGGESTED_QUESTIONS.observation)}</div>
        <div class="qcat-label">Motif &amp; relations</div>
        <div class="quick-questions">${makeChips(SUGGESTED_QUESTIONS.motive)}</div>
        <div class="qcat-label">Chronologie</div>
        <div class="quick-questions">${makeChips(SUGGESTED_QUESTIONS.timeline)}</div>
      </div>
    </div>`;

  if (disabled) return;

  box.querySelectorAll('.q-chip').forEach(chip => {
    chip.addEventListener('click', () => askQuestion(chip.dataset.q, witnessIdx, box, w));
  });
}

async function askQuestion(question, witnessIdx, box, w) {
  if (G.actionsLeft <= 0) return;

  box.querySelectorAll('.q-chip').forEach(c => c.disabled = true);
  setLoading(true);

  try {
    const result = await api('POST', `/${G.trialId}/question/${witnessIdx}`, { question });
    G.actionsLeft--;
    G.lastResponse = result;

    // Révéler la personnalité du témoin après la première question
    G.questionedWitnesses.add(witnessIdx);

    // Mise à jour de la confiance du client
    if (result.contradictionDetected) {
      updateClientConfidence(+12);
      Sounds.success();
    } else {
      updateClientConfidence(+2);
    }

    softRender(result.gameState);
    renderActionDots(G.actionsLeft, PHASE_MAX_ACTIONS.DEFENSE_CASE);
    showWitnessResponse(result);
    rebuildQuestionBox(box, witnessIdx, w);

    // Révéler le badge personnalité sur la carte du témoin interrogé
    const card = document.querySelector(`#witness-cards .witness-card[data-idx="${witnessIdx}"]`);
    if (card) {
      const badgeEl = card.querySelector('.personality-badge');
      const p = PERSONALITY_LABELS[w.personality];
      if (badgeEl && p) {
        badgeEl.textContent = `${p.icon} ${p.label}`;
        badgeEl.classList.remove('unknown');
      }
      card.querySelector('.witness-name').textContent =
        `${witnessEmoji(w, true)} ${w.name}`;
    }
    // Garder le témoin sélectionné
    document.querySelectorAll('#witness-cards .witness-card').forEach(c => {
      c.classList.toggle('selected', parseInt(c.dataset.idx) === witnessIdx);
    });
  } catch (e) {
    toast('Erreur : ' + e.message, 'error');
    box.querySelectorAll('.q-chip').forEach(c => c.disabled = false);
  } finally {
    setLoading(false);
  }
}

function showWitnessResponse(result) {
  const area = document.getElementById('response-area');
  if (!area) return;
  const isContradiction = result.contradictionDetected;
  const cls = isContradiction ? 'response-box contradiction' : 'response-box';
  const responseText = result.witnessResponse || '—';

  area.innerHTML = `
    <div class="${cls}">
      <div class="resp-name">
        ${isContradiction ? '⚡ Contradiction détectée !' : '💬 Le témoin répond'}
      </div>
      <div class="resp-text" id="resp-typewriter"></div>
      ${isContradiction ? `<div class="contradiction-alert">⚡ ${result.message}</div>` : ''}
    </div>`;

  const textEl = document.getElementById('resp-typewriter');
  if (textEl) typewriter(textEl, responseText);

  if (isContradiction) {
    Sounds.contradiction();
    const card = document.querySelector('#witness-cards .witness-card.selected');
    flashCard(card, 'success');
    setTimeout(() => toast('Murmures dans la salle d\'audience…', ''), 400);
  }
}

// ─── Confrontation ────────────────────────────────────────────
function updateConfrontForm() {
  const form  = document.getElementById('confront-form');
  const title = document.getElementById('confront-title');
  if (!form) return;

  const ready = G.confrontSel.length === 2;
  form.classList.toggle('hidden', !ready);

  const btn = document.getElementById('confront-btn');

  if (!ready) {
    if (btn) btn.disabled = true;
    return;
  }

  const w1 = G.state.witnesses[G.confrontSel[0]];
  const w2 = G.state.witnesses[G.confrontSel[1]];
  title.textContent = `Confronter ${w1?.name ?? '?'} et ${w2?.name ?? '?'}`;

  if (btn) {
    // Activer seulement si un sujet a déjà été sélectionné
    btn.disabled = !G.selectedTopic;

    btn.onclick = async () => {
      if (G.actionsLeft <= 0) { toast('Plus de confrontations disponibles.', 'error'); return; }
      if (!G.selectedTopic)   { toast('Choisissez un sujet de confrontation.', 'error'); return; }
      const topic = G.selectedTopic;
      btn.disabled = true;
      setLoading(true);
      try {
        const result = await api('POST', `/${G.trialId}/confront`, {
          witness1: G.confrontSel[0],
          witness2: G.confrontSel[1],
          topic
        });
        G.actionsLeft--;
        G.confrontSel  = [];
        G.selectedTopic = null;
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

        document.querySelectorAll('#witness-cards .witness-card').forEach(c => {
          c.classList.remove('selected');
          c.querySelectorAll('.selected-badge').forEach(b => b.remove());
        });
        form.classList.add('hidden');

        // Réinitialiser les chips et le label
        document.querySelectorAll('.confront-chip').forEach(c => c.classList.remove('selected'));
        const label = document.getElementById('topic-selected-label');
        if (label) label.textContent = '';
      } catch (e) {
        toast('Erreur : ' + e.message, 'error');
        btn.disabled = false;
      } finally {
        setLoading(false);
      }
    };
  }
}

// ════════════════════════════════════════════════════════════
//  Avancement de phase
// ════════════════════════════════════════════════════════════
async function advancePhase() {
  const phase       = G.state?.phase;
  const needsWarning = G.actionsLeft > 0 && !!PHASE_MAX_ACTIONS[phase];

  // Premier clic : avertissement si actions restantes
  if (needsWarning && !_advanceConfirmPending) {
    _advanceConfirmPending = true;
    const btn = document.getElementById('next-btn');
    if (btn) {
      btn.textContent = '⚠ Confirmer et passer →';
      btn.classList.remove('btn-outline');
      btn.classList.add('btn-primary');
    }
    toast(buildPhaseSummary(phase), 'warning');
    return;
  }

  _advanceConfirmPending = false;

  // Afficher un événement dramatique aléatoire (50% de chance, pas en ouverture)
  const currentPhase = G.state?.phase;
  if (currentPhase && currentPhase !== 'OPENING_STATEMENTS') {
    await showPhaseEvent();
  }

  Sounds.gavel();
  setLoading(true);
  try {
    const state = await api('POST', `/${G.trialId}/next`);
    G.selectedWitness = -1;
    G.confrontSel     = [];
    G.lastResponse    = null;
    G.selectedTopic   = null;
    setPhaseActions(state.phase);
    render(state);
  } catch (e) {
    toast('Erreur : ' + e.message, 'error');
  } finally {
    setLoading(false);
  }
}

function showPhaseEvent() {
  return new Promise(resolve => {
    if (Math.random() < 0.5) { resolve(); return; }
    const ev = PHASE_EVENTS[Math.floor(Math.random() * PHASE_EVENTS.length)];
    const overlay = document.createElement('div');
    overlay.className = 'phase-event-overlay';
    overlay.innerHTML = `
      <div class="phase-event-card">
        <span class="pe-icon">${ev.icon}</span>
        <span class="pe-badge pe-${ev.type}">${
          ev.type === 'good'    ? '↓ Favorable à la défense' :
          ev.type === 'bad'     ? '↑ Favorable à l\'accusation' :
                                  '→ Événement neutre'
        }</span>
        <h3 class="pe-title">${ev.title}</h3>
        <p class="pe-desc">${ev.desc}</p>
        <button class="btn-primary pe-continue">Continuer →</button>
      </div>`;
    document.body.appendChild(overlay);
    overlay.querySelector('.pe-continue').addEventListener('click', () => {
      overlay.remove();
      resolve();
    });
  });
}

function setPhaseActions(phase) {
  G.actionsLeft = PHASE_MAX_ACTIONS[phase] ?? 0;
}

function buildPhaseSummary(phase) {
  const n  = G.actionsLeft;
  const pl = n > 1;
  if (phase === 'PROSECUTION_CASE')
    return `Il vous reste ${n} action${pl ? 's' : ''} — certaines preuves n'ont peut-être pas été vérifiées.`;
  if (phase === 'DEFENSE_CASE')
    return `Il vous reste ${n} question${pl ? 's' : ''} — des témoins n'ont pas encore été interrogés.`;
  if (phase === 'CROSS_EXAMINATION')
    return `Il vous reste ${n} confrontation${pl ? 's' : ''} disponible${pl ? 's' : ''}.`;
  return `Il vous reste ${n} action${pl ? 's' : ''} non utilisée${pl ? 's' : ''}.`;
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

  // Grade
  const gradeEl    = document.getElementById('v-grade');
  const gradeLblEl = document.getElementById('v-grade-label');
  const gc = GRADE_COLORS[v.grade] || '#aaa';
  gradeEl.textContent    = v.grade || '?';
  gradeEl.style.color    = gc;
  gradeEl.style.borderColor = gc;
  gradeLblEl.textContent = GRADE_LABELS[v.grade] || '';

  // Feedback
  const fbEl = document.getElementById('v-feedback');
  fbEl.innerHTML = (v.feedback || []).map(f => `<div class="feedback-item">${f}</div>`).join('');

  document.getElementById('v-explanation').textContent = v.explanation;

  // Sons et statistiques
  if (v.status === 'NOT_GUILTY') { SESSION.wins++; setTimeout(() => Sounds.verdictWin(), 400); }
  else                            { SESSION.losses++; setTimeout(() => Sounds.verdictLose(), 400); }
  updateSessionStats();

  document.getElementById('verdict-overlay').classList.remove('hidden');
}

function updateSessionStats() {
  const el = document.getElementById('session-stats');
  if (!el) return;
  const total = SESSION.wins + SESSION.losses;
  if (total === 0) { el.innerHTML = ''; return; }
  el.innerHTML = `
    <span class="sess-win">⚖ ${SESSION.wins} victoire${SESSION.wins !== 1 ? 's' : ''}</span>
    <span class="sess-sep">·</span>
    <span class="sess-loss">${SESSION.losses} défaite${SESSION.losses !== 1 ? 's' : ''}</span>
    <span class="sess-sep">·</span>
    <span class="sess-total">${total} affaire${total !== 1 ? 's' : ''}</span>`;
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
