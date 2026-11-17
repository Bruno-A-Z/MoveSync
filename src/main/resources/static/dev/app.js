(() => {
  'use strict';

  const BASE = (document.body.dataset.base || '/').replace(/\/$/, '');
  const $ = (sel, raiz = document) => raiz.querySelector(sel);
  const esc = (v) => String(v ?? '').replace(/[&<>"']/g,
    (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const hora = (iso) => (iso ? new Date(iso).toLocaleTimeString('pt-BR') : '—');
  const num = (v, padrao = 0) => { const n = parseInt(v, 10); return Number.isNaN(n) ? padrao : n; };
  const rand = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min;

  // ---------- sessão (sessionStorage: some ao fechar a aba) ----------
  const sessao = {
    get token() { return sessionStorage.getItem('token'); },
    get perfil() { return sessionStorage.getItem('perfil'); },
    get nome() { return sessionStorage.getItem('nome'); },
    salvar(t) {
      sessionStorage.setItem('token', t.token);
      sessionStorage.setItem('perfil', t.perfil);
      sessionStorage.setItem('nome', t.nome);
    },
    limpar() { sessionStorage.clear(); },
  };

  let cruzamentoId = null;
  let vias = [];
  let timerStatus = null;
  let timerSim = null;
  let rodadaSim = 0;
  const logs = [];

  // ---------- toast ----------
  let timerToast = null;
  function toast(msg, erro = false) {
    const el = $('#toast');
    el.textContent = msg;
    el.classList.toggle('erro', erro);
    el.hidden = false;
    clearTimeout(timerToast);
    timerToast = setTimeout(() => { el.hidden = true; }, 4500);
  }

  // ---------- chamada à API + console ----------
  async function api(metodo, caminho, corpo) {
    const t0 = performance.now();
    const headers = { Accept: 'application/json' };
    if (corpo !== undefined) headers['Content-Type'] = 'application/json';
    if (sessao.token) headers.Authorization = 'Bearer ' + sessao.token;

    let resp;
    let dados = null;
    try {
      resp = await fetch(BASE + caminho, {
        method: metodo,
        headers,
        body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
      });
      const texto = await resp.text();
      try { dados = texto ? JSON.parse(texto) : null; } catch { dados = texto; }
    } catch (e) {
      registrar(metodo, caminho, corpo, 0, { erro: String(e) }, performance.now() - t0);
      toast('Falha de rede: ' + e, true);
      throw e;
    }
    registrar(metodo, caminho, corpo, resp.status, dados, performance.now() - t0);

    if (resp.status === 401 && sessao.token) {
      sair();
      toast('Sessão expirada. Entre novamente.', true);
    }
    if (!resp.ok) {
      const campos = (dados && dados.campos || []).map((c) => `${c.campo}: ${c.mensagem}`).join('; ');
      const err = new Error((dados && dados.mensagem || 'HTTP ' + resp.status) + (campos ? ' (' + campos + ')' : ''));
      err.status = resp.status;
      throw err;
    }
    return dados;
  }

  function registrar(metodo, caminho, corpo, status, dados, ms) {
    let req = corpo;
    if (req && typeof req === 'object' && 'senha' in req) req = { ...req, senha: '***' }; // nunca logar senha
    logs.unshift({ metodo, caminho, req, status, dados, ms: Math.round(ms), quando: new Date() });
    if (logs.length > 40) logs.pop();
    renderConsole();
  }

  function renderConsole() {
    const el = $('#console');
    if (!logs.length) { el.className = 'vazio'; el.textContent = 'Nenhuma requisição ainda.'; return; }
    el.className = '';
    el.innerHTML = logs.map((l) => {
      const ok = l.status >= 200 && l.status < 300;
      const corpoReq = l.req !== undefined ? '→ ' + JSON.stringify(l.req, null, 2) + '\n\n' : '';
      return `<details><summary>${esc(l.quando.toLocaleTimeString('pt-BR'))} · <b>${esc(l.metodo)}</b> ${esc(l.caminho)} ` +
        `<span class="${ok ? 's-ok' : 's-erro'}">${esc(l.status || 'ERRO')}</span> ${esc(l.ms)} ms</summary>` +
        `<pre>${esc(corpoReq + '← ' + JSON.stringify(l.dados, null, 2))}</pre></details>`;
    }).join('');
  }

  // ---------- visibilidade por perfil ----------
  function aplicarPerfil() {
    const logado = !!sessao.token;
    const perfil = (sessao.perfil || '').toLowerCase();
    document.querySelectorAll('[data-so]').forEach((el) => {
      const so = el.dataset.so;
      const mostrar = so === 'logado' ? logado : so === 'deslogado' ? !logado : logado && perfil === so;
      el.hidden = !mostrar;
    });
    $('#sessao-info').textContent = logado ? `${sessao.nome} · ${sessao.perfil}` : 'não autenticado';
  }

  // ---------- login / registro / sair ----------
  async function autenticar(caminho, corpo) {
    const t = await api('POST', caminho, corpo);
    sessao.salvar(t);
    aplicarPerfil();
    toast('Autenticado como ' + t.perfil);
    await iniciar();
  }

  function sair() {
    sessao.limpar();
    pararTimers();
    cruzamentoId = null;
    vias = [];
    aplicarPerfil();
  }

  function pararTimers() {
    clearInterval(timerStatus); timerStatus = null;
    clearInterval(timerSim); timerSim = null;
    const a = $('#auto-sim'); if (a) a.checked = false;
  }

  // ---------- cruzamentos / vias ----------
  async function carregarCruzamentos() {
    const pagina = await api('GET', '/api/cruzamentos?size=100&sort=nome,asc');
    const lista = pagina.content || [];
    const opcoes = lista.map((c) => `<option value="${esc(c.id)}">${esc(c.nome)}</option>`).join('');
    $('#sel-cruzamento').innerHTML = opcoes;
    $('#rel-cruzamento').innerHTML = opcoes;
    if (!lista.length) { $('#status').textContent = 'Nenhum cruzamento cadastrado.'; return; }
    await selecionarCruzamento(lista[0].id);
  }

  async function selecionarCruzamento(id) {
    cruzamentoId = id;
    $('#sel-cruzamento').value = String(id);
    await carregarVias();
    await Promise.all([atualizarStatus(), carregarDecisoes()]);
  }

  async function carregarVias() {
    const pagina = await api('GET', `/api/vias?cruzamentoId=${cruzamentoId}&size=100&sort=id,asc`);
    vias = pagina.content || [];
    $('#tabela-sim tbody').innerHTML = vias.map((v) => `
      <tr data-via="${esc(v.id)}">
        <td>${esc(v.nome)} <span class="dica">${esc(v.sentido)}</span></td>
        <td><input type="number" class="v" min="0" max="500" value="5"></td>
        <td><input type="number" class="f" min="0" max="2000" value="20"></td>
        <td><input type="checkbox" class="e"></td>
        <td>
          <button type="button" class="secundario mini" data-acao="congestionar" data-via="${esc(v.id)}">congestionar</button>
          <button type="button" class="secundario mini" data-acao="emergencia" data-via="${esc(v.id)}">emergência</button>
        </td>
      </tr>`).join('');
  }

  // ---------- status (visão do cidadão) ----------
  async function atualizarStatus() {
    if (!cruzamentoId) return;
    try {
      const s = await api('GET', `/api/cruzamentos/${cruzamentoId}/status`);
      const linhas = (s.vias || []).map((v) => `<tr><td>${esc(v.nome)} <span class="dica">${esc(v.sentido)}</span></td>` +
        `<td>${esc(v.quantidadeVeiculos ?? '—')}</td><td>${esc(v.tamanhoFilaMetros ?? '—')}</td><td>${esc(hora(v.atualizadoEm))}</td></tr>`).join('');
      const d = s.ultimaDecisao;
      const decisao = d
        ? `<div class="decisao"><span class="badge b-${esc(d.modo)}">${esc(d.modo)}</span> ` +
          `verde para <b>${esc(d.viaPrioritariaNome)}</b><div class="tempo">${esc(d.tempoVerdeSegundos)} s</div>` +
          `<div>${esc(d.explicacao)}</div><div class="dica">${esc(hora(d.criadoEm))}</div></div>`
        : '<p class="vazio">Ainda sem decisão. Envie leituras no simulador (gestor).</p>';
      $('#status').className = '';
      $('#status').innerHTML = `<b>${esc(s.nome)}</b> <span class="dica">${esc(s.endereco)}</span>` +
        `<table><thead><tr><th>Via</th><th>Veículos</th><th>Fila (m)</th><th>Leitura</th></tr></thead><tbody>${linhas}</tbody></table>${decisao}`;
    } catch (e) { /* o console e o toast já mostram o erro */ }
  }

  async function carregarDecisoes() {
    if (!cruzamentoId) return;
    try {
      const p = await api('GET', `/api/decisoes?cruzamentoId=${cruzamentoId}&size=10`);
      const l = p.content || [];
      const el = $('#decisoes');
      if (!l.length) { el.className = 'vazio'; el.textContent = 'Sem decisões ainda.'; return; }
      el.className = '';
      el.innerHTML = '<table><thead><tr><th>Hora</th><th>Modo</th><th>Via</th><th>Verde</th><th>Explicação</th></tr></thead><tbody>' +
        l.map((d) => `<tr><td>${esc(hora(d.criadoEm))}</td><td><span class="badge b-${esc(d.modo)}">${esc(d.modo)}</span></td>` +
          `<td>${esc(d.viaPrioritariaNome)}</td><td>${esc(d.tempoVerdeSegundos)} s</td><td>${esc(d.explicacao)}</td></tr>`).join('') +
        '</tbody></table>';
    } catch (e) { /* idem */ }
  }

  // ---------- simulador (gestor) ----------
  function linhaDaVia(id) { return $(`#tabela-sim tr[data-via="${id}"]`); }

  function preencher(viaId, { veiculos, fila, emergencia }) {
    const tr = linhaDaVia(viaId);
    if (!tr) return;
    $('.v', tr).value = veiculos;
    $('.f', tr).value = fila;
    $('.e', tr).checked = !!emergencia;
  }

  function cenario(viaEspecial, tipo) {
    vias.forEach((v) => {
      if (v.id === viaEspecial && tipo === 'congestionar') preencher(v.id, { veiculos: rand(22, 35), fila: rand(90, 180), emergencia: false });
      else if (v.id === viaEspecial && tipo === 'emergencia') preencher(v.id, { veiculos: rand(1, 4), fila: rand(5, 20), emergencia: true });
      else preencher(v.id, { veiculos: rand(0, 8), fila: rand(0, 30), emergencia: false });
    });
  }

  async function enviarLeituras() {
    const botao = $('#btn-enviar');
    botao.disabled = true;
    try {
      for (const v of vias) {
        const tr = linhaDaVia(v.id);
        await api('POST', '/api/leituras', {
          viaId: v.id,
          quantidadeVeiculos: Math.min(500, Math.max(0, num($('.v', tr).value))),
          tamanhoFilaMetros: Math.min(2000, Math.max(0, num($('.f', tr).value))),
          veiculoEmergencia: $('.e', tr).checked,
        });
      }
      toast(`${vias.length} leitura(s) publicada(s) no RabbitMQ (202)`);
      setTimeout(() => { atualizarStatus(); carregarDecisoes(); }, 1500); // consumo é assíncrono
    } catch (e) {
      toast('Erro ao enviar leituras: ' + e.message, true);
    } finally {
      botao.disabled = false;
    }
  }

  function alternarSimulacao(ligar) {
    clearInterval(timerSim); timerSim = null;
    if (!ligar) return;
    const passo = async () => {
      if (!vias.length) return;
      const congestionada = vias[Math.floor(rodadaSim / 5) % vias.length].id; // a cada 5 rodadas troca a via cheia
      cenario(congestionada, 'congestionar');
      rodadaSim += 1;
      await enviarLeituras();
    };
    passo();
    timerSim = setInterval(passo, 3000);
  }

  // ---------- relatos ----------
  async function carregarRelatos() {
    const gestor = sessao.perfil === 'GESTOR';
    try {
      const p = await api('GET', gestor ? '/api/relatos?size=10' : '/api/relatos/meus?size=10');
      const l = p.content || [];
      const el = $('#relatos');
      if (!l.length) { el.className = 'vazio'; el.textContent = 'Sem relatos.'; return; }
      el.className = '';
      el.innerHTML = '<table><thead><tr><th>Cruzamento</th><th>Categoria (IA)</th><th>Gravidade</th><th>Resumo (IA)</th><th>Status</th></tr></thead><tbody>' +
        l.map((r) => {
          const status = gestor
            ? `<select data-relato="${esc(r.id)}">${['ABERTO', 'EM_ANALISE', 'RESOLVIDO']
                .map((s) => `<option value="${s}"${s === r.status ? ' selected' : ''}>${s}</option>`).join('')}</select>`
            : `<span class="badge b-${esc(r.status)}">${esc(r.status)}</span>`;
          return `<tr><td>${esc(r.cruzamentoNome)}</td><td>${esc(r.categoria)}</td><td>${esc(r.gravidade)}/5</td>` +
            `<td>${esc(r.resumoIa)}<div class="dica">${esc(r.descricao)}</div></td><td>${status}</td></tr>`;
        }).join('') + '</tbody></table>';
    } catch (e) { /* idem */ }
  }

  // ---------- inicialização ----------
  async function iniciar() {
    clearInterval(timerStatus);
    try {
      await carregarCruzamentos();
      await carregarRelatos();
    } catch (e) { return; }
    if ($('#auto-status').checked) {
      timerStatus = setInterval(() => { atualizarStatus(); carregarDecisoes(); carregarRelatos(); }, 3000);
    }
  }

  // ---------- eventos ----------
  $('#form-login').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    try { await autenticar('/api/auth/login', { email: $('#login-email').value.trim(), senha: $('#login-senha').value }); }
    catch (e) { toast(e.message, true); }
  });

  document.querySelectorAll('button.rapido').forEach((b) => b.addEventListener('click', async () => {
    $('#login-email').value = b.dataset.email;
    $('#login-senha').value = b.dataset.senha;
    try { await autenticar('/api/auth/login', { email: b.dataset.email, senha: b.dataset.senha }); }
    catch (e) { toast(e.message, true); }
  }));

  $('#form-registro').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    try {
      await autenticar('/api/auth/registro', {
        nome: $('#reg-nome').value.trim(), email: $('#reg-email').value.trim(), senha: $('#reg-senha').value,
      });
    } catch (e) { toast(e.message, true); }
  });

  $('#btn-sair').addEventListener('click', sair);
  $('#sel-cruzamento').addEventListener('change', (ev) => selecionarCruzamento(Number(ev.target.value)));
  $('#btn-atualizar').addEventListener('click', () => { atualizarStatus(); carregarDecisoes(); });
  $('#auto-status').addEventListener('change', (ev) => {
    clearInterval(timerStatus); timerStatus = null;
    if (ev.target.checked && sessao.token) {
      timerStatus = setInterval(() => { atualizarStatus(); carregarDecisoes(); carregarRelatos(); }, 3000);
    }
  });
  $('#btn-enviar').addEventListener('click', enviarLeituras);
  $('#auto-sim').addEventListener('change', (ev) => alternarSimulacao(ev.target.checked));
  $('#btn-limpar-log').addEventListener('click', () => { logs.length = 0; renderConsole(); });

  $('#tabela-sim').addEventListener('click', async (ev) => {
    const b = ev.target.closest('button[data-acao]');
    if (!b) return;
    cenario(Number(b.dataset.via), b.dataset.acao);
    await enviarLeituras();
  });

  $('#form-relato').addEventListener('submit', async (ev) => {
    ev.preventDefault();
    try {
      const r = await api('POST', '/api/relatos', {
        cruzamentoId: Number($('#rel-cruzamento').value),
        descricao: $('#rel-descricao').value.trim(),
      });
      toast('Relato registrado. A IA classifica em segundo plano; a lista atualiza sozinha.');
      $('#rel-descricao').value = '';
      await carregarRelatos();
    } catch (e) { toast(e.message, true); }
  });

  $('#relatos').addEventListener('change', async (ev) => {
    const sel = ev.target.closest('select[data-relato]');
    if (!sel) return;
    try {
      await api('PATCH', `/api/relatos/${sel.dataset.relato}/status`, { status: sel.value });
      toast('Status atualizado');
    } catch (e) { toast(e.message, true); await carregarRelatos(); }
  });

  aplicarPerfil();
  renderConsole();
  if (sessao.token) iniciar();
})();
