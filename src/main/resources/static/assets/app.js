'use strict';

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const money = value => new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' }).format(value);
const escapeHtml = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const cents = value => Math.round(Number(value) * 100);
const sum = rows => rows.reduce((total, row) => total + cents(row.amount), 0) / 100;
const today = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Lima', year:'numeric',month:'2-digit',day:'2-digit' }).format(new Date());
const prettyDate = date => new Intl.DateTimeFormat('es-PE', { day:'2-digit', month:'short',year:'numeric' }).format(new Date(date+'T12:00:00'));
const monthTitle = month => new Intl.DateTimeFormat('es-PE', {month:'long',year:'numeric'}).format(new Date(month+'-01T12:00:00'));
const palette = ['#245a49','#9ec568','#e2b76c','#77a4aa','#8c8bbb','#c78466','#81ba99','#9ba5ae'];
let state, csrf, page = 'dashboard', authMode = 'login', charts = [], editContext = null, confirmAction = null, toastTimer;
let filters = { text:'', kind:'', category:'', index:0 }, adminAccounts=[];

try { document.body.classList.toggle('dark', localStorage.getItem('brujula-theme') === 'dark'); } catch {}
$('#month').value = today().slice(0,7);

function toast(message) {
    clearTimeout(toastTimer); $('#toast').textContent=message; $('#toast').classList.remove('hidden');
    toastTimer=setTimeout(() => $('#toast').classList.add('hidden'),4500);
}
async function getCsrf() {
    const response=await fetch('/api/csrf',{credentials:'same-origin',cache:'no-store'});
    if(!response.ok) throw new Error('No se pudo conectar. Comprueba que Brújula esté en ejecución.');
    csrf=await response.json();
}
async function api(path, method='GET', data=null) {
    const headers={};
    if(method!=='GET') { if(!csrf) await getCsrf(); headers[csrf.headerName]=csrf.token; }
    if(data!==null) headers['Content-Type']='application/json';
    const response=await fetch('/api'+path,{method,headers,credentials:'same-origin',cache:'no-store',body:data===null?undefined:JSON.stringify(data)});
    if(!response.ok) {
        if(response.status===401) { showAuth(); throw new Error('Tu sesión terminó. Vuelve a iniciar sesión.'); }
        let error; try { error=await response.json(); } catch {}
        if(response.status===403) { await getCsrf(); throw new Error('La sesión cambió. Intenta guardar de nuevo.'); }
        throw new Error(error?.message || 'No se pudo completar la operación. Inténtalo de nuevo.');
    }
    if(response.status===204 || !response.headers.get('content-type')?.includes('application/json')) return null;
    return response.json();
}
function showAuth() {
    state=null; $('#loading').classList.add('hidden'); $('#application').classList.add('hidden'); $('#auth').classList.remove('hidden');
    if($('#editor').open) $('#editor').close();
    if($('#confirmation').open) $('#confirmation').close();
    charts.forEach(c=>c.destroy()); charts=[];
}
async function refresh() {
    state=await api('/state');
    $('#loading').classList.add('hidden'); $('#auth').classList.add('hidden'); $('#application').classList.remove('hidden');
    $('#header-name').textContent=state.user.name;
    $('#avatar').textContent=state.user.name.trim().split(/\s+/).slice(0,2).map(x=>x[0]).join('').toUpperCase();
    render();
}
function setAuthMode(mode) {
    authMode=mode; const register=mode==='register';
    $('#name-label').classList.toggle('hidden',!register); $('#auth-form').elements.name.required=register;
    $('#password-help').classList.toggle('hidden',!register);
    $('#auth-form').elements.password.minLength=register?10:1;
    $('#auth-form').elements.password.autocomplete=register?'new-password':'current-password';
    $('#auth-title').textContent=register?'Empieza con dirección.':'Qué bueno verte.';
    $('#auth-subtitle').textContent=register?'Crea tu cuenta y organiza lo que viene.':'Entra y continúa con tus planes.';
    $('#auth-submit').innerHTML=register?'Crear mi cuenta <span>→</span>':'Entrar a mi espacio <span>→</span>';
    $('#login-tab').classList.toggle('active',!register); $('#register-tab').classList.toggle('active',register); $('#auth-error').textContent='';
}
$('#login-tab').addEventListener('click',()=>setAuthMode('login'));
$('#register-tab').addEventListener('click',()=>setAuthMode('register'));
$('#show-password').addEventListener('click',()=> {
    const input=$('#auth-form').elements.password; const show=input.type==='password'; input.type=show?'text':'password';
    $('#show-password').textContent=show?'Ocultar':'Ver'; $('#show-password').setAttribute('aria-label',show?'Ocultar contraseña':'Mostrar contraseña');
});
$('#auth-form').addEventListener('submit',async event=> {
    event.preventDefault(); const button=$('#auth-submit');button.disabled=true; $('#auth-error').textContent='';
    const data=Object.fromEntries(new FormData(event.target)); data.email=data.email.trim().toLowerCase();
    try {
        await getCsrf();
        if(authMode==='register') await api('/register','POST',data);
        const body=new URLSearchParams({email:data.email,password:data.password});
        const response=await fetch('/api/login',{method:'POST',headers:{[csrf.headerName]:csrf.token},body,credentials:'same-origin'});
        if(!response.ok) throw new Error(response.status===401?'El correo o la contraseña no coinciden.':'No se pudo iniciar sesión. Inténtalo de nuevo.');
        await getCsrf(); filters={text:'',kind:'',category:'',index:0}; await refresh();
        event.target.elements.password.value='';
    } catch(error) { $('#auth-error').textContent=error.message; } finally { button.disabled=false; }
});
$('#logout').addEventListener('click',async()=> {try {await api('/logout','POST');showAuth();await getCsrf();}catch(e){toast(e.message);}});
$$('.theme-toggle').forEach(button=>button.addEventListener('click',()=> {
    document.body.classList.toggle('dark'); try{localStorage.setItem('brujula-theme',document.body.classList.contains('dark')?'dark':'light');}catch{}
    if(state) render();
}));
$('#menu-toggle').addEventListener('click',()=> {
    const open=$('#sidebar').classList.toggle('open');$('#menu-toggle').setAttribute('aria-expanded',String(open));
});
document.addEventListener('click',event=> {
    if(!event.target.closest('#sidebar')&&!event.target.closest('#menu-toggle')) {
        $('#sidebar').classList.remove('open');$('#menu-toggle').setAttribute('aria-expanded','false');
    }
});
window.addEventListener('hashchange',()=> { if(state) {render();$('#main').focus();} });
$('#month').addEventListener('change',()=> {if(!$('#month').value)$('#month').value=today().slice(0,7);filters.index=0;if(state)render();});
$('#new-movement').addEventListener('click',()=>openEditor('movement'));
function confirm(title,message,label,action) {
    $('#confirm-title').textContent=title;$('#confirm-message').textContent=message;$('#confirm-ok').textContent=label;
    confirmAction=action;$('#confirmation').showModal();
}
$('#confirm-cancel').addEventListener('click',()=>$('#confirmation').close());
$('#confirm-ok').addEventListener('click',async()=> {
    const button=$('#confirm-ok');button.disabled=true;
    try {await confirmAction();$('#confirmation').close();}catch(e){toast(e.message);}finally{button.disabled=false;}
});
$('#load-sample').addEventListener('click',()=>confirm('¿Explorar con ejemplos?','Se añadirán ingresos, gastos, presupuestos y metas ficticios a tu cuenta. Podrás editarlos o eliminarlos.','Cargar ejemplos',async()=> {
    await api('/sample','POST');await refresh();toast('Los datos de ejemplo ya están listos.');
}));
function monthly() { return state.movements.filter(m=>m.date.slice(0,7)===$('#month').value); }
function totals(rows) {const income=sum(rows.filter(m=>m.kind==='INGRESO')),expense=sum(rows.filter(m=>m.kind==='GASTO'));return {income,expense,balance:(cents(income)-cents(expense))/100};}
function blank(title,text) {return `<div class="empty"><strong>${escapeHtml(title)}</strong>${escapeHtml(text)}</div>`;}
function stat(label,value,note,icon,featured=false) {return `<article class="stat ${featured?'featured':''}"><div class="stat-top"><span>${label}</span><span class="stat-icon">${icon}</span></div><div class="stat-value">${money(value)}</div><p class="stat-note">${escapeHtml(note)}</p></article>`;}
function render() {
    if(!state)return;
    page=location.hash.slice(1)||'dashboard';if(!['dashboard','movements','budgets','goals','reports','categories','admin'].includes(page))page='dashboard';
    if(page==='admin'&&state.user.role!=='ADMIN')page='dashboard';
    $('#admin-link').classList.toggle('hidden',state.user.role!=='ADMIN');
    charts.forEach(chart=>chart.destroy());charts=[];
    $$('.page').forEach(section=>section.classList.add('hidden'));$('#page-'+page).classList.remove('hidden');
    $$('[data-page]').forEach(link=> {link.classList.toggle('active',link.dataset.page===page);if(link.dataset.page===page)link.setAttribute('aria-current','page');else link.removeAttribute('aria-current');});
    $('#sidebar').classList.remove('open');$('#menu-toggle').setAttribute('aria-expanded','false');
    const headings={dashboard:[`Hola, ${state.user.name.split(' ')[0]}.`,'Así va tu dinero. Cada movimiento cuenta.','TU PANORAMA FINANCIERO'],movements:['Tus movimientos','Todo lo que entra y sale, en un solo lugar.','EL DÍA A DÍA'],budgets:['Un plan para tus gastos','Pon un límite por categoría y sigue tu progreso.','GASTA CON INTENCIÓN'],goals:['Lo que quieres lograr','Dale nombre a tus planes y sigue tu ahorro reservado.','PIENSA EN LO QUE VIENE'],reports:['Tus números, más claros','Descarga un PDF del mes seleccionado.','UNA MIRADA CON PERSPECTIVA'],categories:['Cada cosa en su lugar','Organiza tus movimientos a tu manera.','TU PROPIA ORGANIZACIÓN']};
    headings.admin=['Administración','Gestiona el acceso sin consultar las finanzas de otras personas.','GESTIÓN DE CUENTAS'];
    $('#page-title').textContent=headings[page][0];$('#page-subtitle').textContent=headings[page][1];$('#page-eyebrow').textContent=headings[page][2];
    $('.month-picker').classList.toggle('hidden',['goals','categories','admin'].includes(page));
    $('#new-movement').classList.toggle('hidden',page==='admin');
    $('#empty-welcome').classList.toggle('hidden',page!=='dashboard'||state.movements.length>0||state.budgets.length>0||state.goals.length>0);
    ({dashboard:renderDashboard,movements:renderMovements,budgets:renderBudgets,goals:renderGoals,reports:renderReports,categories:renderCategories,admin:renderAdmin}[page])();
}
function budgetBlock(b,actions=false) {
    const spent=sum(state.movements.filter(m=>m.kind==='GASTO'&&m.categoryId===b.categoryId&&m.date.slice(0,7)===b.month));
    const percent=Math.round(spent/Number(b.amount)*100), remaining=(cents(b.amount)-cents(spent))/100;
    return `<div class="budget-item"><div class="budget-top"><strong>${escapeHtml(b.category)}</strong><span>${percent}%</span></div><div class="progress ${percent>100?'over':percent>=85?'warning':''}" role="progressbar" aria-label="Presupuesto de ${escapeHtml(b.category)}" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${Math.min(percent,100)}"><span style="width:${Math.min(percent,100)}%"></span></div><div class="budget-bottom"><span>${money(spent)} de ${money(b.amount)}</span></div>${actions?`<p class="muted" style="font-size:13px;margin-top:14px">${remaining>=0?'Te quedan '+money(remaining):'Superaste el límite por '+money(-remaining)}</p><div class="card-actions"><button class="secondary" data-action="edit-budget" data-id="${b.id}">Cambiar límite</button><button class="quiet" data-action="delete-budgets" data-id="${b.id}" aria-label="Eliminar presupuesto">×</button></div>`:''}</div>`;
}
function movementTable(rows,full=false) {
    if(!rows.length)return blank('Sin movimientos por aquí','Registra un movimiento o prueba con otro mes o filtro.');
    return `<div class="table-scroll"><table><thead><tr><th>MOVIMIENTO</th><th>FECHA</th>${full?'<th>TIPO</th><th>MÉTODO</th>':''}<th style="text-align:right">MONTO</th>${full?'<th><span class="sr-only">Acciones</span></th>':''}</tr></thead><tbody>${rows.map(m=>`<tr><td><div class="movement-name"><span class="category-icon">${m.kind==='INGRESO'?'↙':'↗'}</span><div><strong>${escapeHtml(m.description)}</strong><small>${escapeHtml(m.category)}</small></div></div></td><td class="date-cell">${prettyDate(m.date)}</td>${full?`<td><span class="badge ${m.kind==='GASTO'?'expense':''}">${m.kind==='GASTO'?'Gasto':'Ingreso'}</span></td><td class="date-cell">${escapeHtml(m.paymentMethod)}</td>`:''}<td class="amount ${m.kind==='INGRESO'?'income':'expense'}">${m.kind==='INGRESO'?'+':'−'} ${money(m.amount)}</td>${full?`<td><div class="row-actions"><button class="quiet" data-action="edit-movement" data-id="${m.id}" aria-label="Editar ${escapeHtml(m.description)}">✎</button><button class="quiet" data-action="delete-movements" data-id="${m.id}" aria-label="Eliminar ${escapeHtml(m.description)}">×</button></div></td>`:''}</tr>`).join('')}</tbody></table></div>`;
}
function renderDashboard() {
    const rows=monthly(),t=totals(rows),all=totals(state.movements),budgets=state.budgets.filter(b=>b.month===$('#month').value);
    $('#page-dashboard').innerHTML=`<div class="stats-grid">${stat('Saldo acumulado',all.balance,'Todo tu historial registrado','↗',true)}${stat('Ingresos del mes',t.income,'Dinero que entró este mes','↓')}${stat('Gastos del mes',t.expense,'Dinero que salió este mes','↑')}${stat('Balance del mes',t.balance,t.balance>=0?'Ingresos menos gastos':'Los gastos superan los ingresos','◈')}</div>
    <div class="dashboard-grid"><article class="panel"><div class="panel-head"><div><h2>Tu dinero en el tiempo</h2><p>Últimos seis meses hasta ${escapeHtml(monthTitle($('#month').value))}</p></div><div class="chart-legend"><span><i class="dot" style="background:#245a49"></i>Ingresos</span><span><i class="dot" style="background:#b8d784"></i>Gastos</span></div></div><div class="chart-wrap"><canvas id="trend-chart" role="img" aria-label="Ingresos y gastos de los últimos seis meses"></canvas></div><details class="muted" style="font-size:12px;margin-top:12px"><summary>Ver cifras de la gráfica</summary><div id="chart-data"></div></details></article>
    <article class="panel"><div class="panel-head"><div><h2>¿A dónde se fue?</h2><p>Gastos por categoría</p></div><span class="badge">Este mes</span></div>${t.expense?'<div class="donut-wrap"><canvas id="category-chart" role="img" aria-label="Distribución de gastos por categoría"></canvas></div><div id="distribution" class="distribution-list"></div>':blank('Todo por descubrir','Tus categorías aparecerán al registrar gastos.')}</article></div>
    <div class="lower-grid"><article class="panel"><div class="panel-head"><h2>Últimos movimientos</h2><a class="text-link" href="#movements">Ver todos →</a></div>${movementTable(rows.slice(0,5))}</article><article class="panel"><div class="panel-head"><h2>Tus presupuestos</h2><a class="text-link" href="#budgets">Administrar →</a></div>${budgets.length?budgets.slice(0,4).map(b=>budgetBlock(b)).join(''):blank('Un límite, más claridad','Crea tu primer presupuesto por categoría.')}</article></div>`;
    const labels=[],incomes=[],expenses=[];const chosen=new Date($('#month').value+'-01T12:00:00');
    for(let i=5;i>=0;i--){const d=new Date(chosen.getFullYear(),chosen.getMonth()-i,1,12);const key=d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0');labels.push(d.toLocaleDateString('es-PE',{month:'short'}));const v=totals(state.movements.filter(m=>m.date.slice(0,7)===key));incomes.push(v.income);expenses.push(v.expense);}
    $('#chart-data').innerHTML=`<table><thead><tr><th>Mes</th><th>Ingresos</th><th>Gastos</th></tr></thead><tbody>${labels.map((label,i)=>`<tr><td>${label}</td><td>${money(incomes[i])}</td><td>${money(expenses[i])}</td></tr>`).join('')}</tbody></table>`;
    if(typeof Chart==='undefined'){toast('No se pudieron cargar las gráficas. Las cifras siguen disponibles.');return;}
    const muted=getComputedStyle(document.body).getPropertyValue('--muted').trim(),line=getComputedStyle(document.body).getPropertyValue('--line').trim();
    charts.push(new Chart($('#trend-chart'),{type:'bar',data:{labels,datasets:[{label:'Ingresos',data:incomes,backgroundColor:'#245a49',borderRadius:5,maxBarThickness:23},{label:'Gastos',data:expenses,backgroundColor:'#b8d784',borderRadius:5,maxBarThickness:23}]},options:{responsive:true,maintainAspectRatio:false,animation:false,plugins:{legend:{display:false},tooltip:{callbacks:{label:c=>`${c.dataset.label}: ${money(c.raw)}`}}},scales:{x:{grid:{display:false},border:{display:false},ticks:{color:muted}},y:{beginAtZero:true,grid:{color:line},border:{display:false},ticks:{color:muted,maxTicksLimit:5,callback:v=>'S/ '+v}}}}}));
    if(t.expense){const grouped={};rows.filter(m=>m.kind==='GASTO').forEach(m=>grouped[m.category]=(grouped[m.category]||0)+cents(m.amount));const entries=Object.entries(grouped).sort((a,b)=>b[1]-a[1]);
        $('#distribution').innerHTML=entries.map(([name,value],i)=>`<div class="distribution-row"><span><i class="dot" style="background:${palette[i%palette.length]}"></i>${escapeHtml(name)}</span><strong>${money(value/100)} <span class="muted" style="font-weight:400">· ${Math.round(value/(t.expense*100)*100)}%</span></strong></div>`).join('');
        charts.push(new Chart($('#category-chart'),{type:'doughnut',data:{labels:entries.map(x=>x[0]),datasets:[{data:entries.map(x=>x[1]/100),backgroundColor:entries.map((_,i)=>palette[i%palette.length]),borderWidth:4,borderColor:getComputedStyle(document.body).getPropertyValue('--surface').trim(),borderRadius:4}]},options:{responsive:true,maintainAspectRatio:false,animation:false,cutout:'72%',plugins:{legend:{display:false},tooltip:{callbacks:{label:c=>`${c.label}: ${money(c.raw)}`}}}}}));
    }
}
function renderMovements() {
    $('#page-movements').innerHTML=`<article class="panel"><div class="filters"><input id="search-movements" type="search" aria-label="Buscar movimiento" placeholder="Buscar un movimiento…" value="${escapeHtml(filters.text)}"><select id="filter-kind" aria-label="Filtrar por tipo"><option value="">Todos los tipos</option value="GASTO">Gastos</option><option value="INGRESO">Ingresos</option></select><select id="filter-category" aria-label="Filtrar por categoría"><option value="">Todas las categorías</option>${state.categories.map(c=>`<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('')}</select></div><div id="movement-results"></div></article>`;
    $('#filter-kind').value=filters.kind;$('#filter-category').value=filters.category;
    $('#search-movements').addEventListener('input',e=>{filters.text=e.target.value;filters.index=0;renderMovementResults();});
    $('#filter-kind').addEventListener('change',e=>{filters.kind=e.target.value;filters.index=0;renderMovementResults();});
    $('#filter-category').addEventListener('change',e=>{filters.category=e.target.value;filters.index=0;renderMovementResults();});
    renderMovementResults();
}
function renderMovementResults() {
    const rows=monthly().filter(m=>(!filters.kind||m.kind===filters.kind)&&(!filters.category||String(m.categoryId)===filters.category)&&(`${m.description} ${m.category}`).toLocaleLowerCase().includes(filters.text.toLocaleLowerCase()));
    const pages=Math.max(1,Math.ceil(rows.length/12));filters.index=Math.min(filters.index,pages-1);const start=filters.index*12;
    $('#movement-results').innerHTML=movementTable(rows.slice(start,start+12),true)+`<div class="pagination"><span>${rows.length} movimientos · Página ${filters.index+1} de ${pages}</span><div><button class="quiet" data-action="prev" ${filters.index===0?'disabled':''} aria-label="Página anterior">←</button><button class="quiet" data-action="next" ${filters.index===pages-1?'disabled':''} aria-label="Página siguiente">→</button></div></div>`;
}
function renderBudgets() {
    const budgets=state.budgets.filter(b=>b.month===$('#month').value);
    $('#page-budgets').innerHTML=`<div class="section-toolbar"><p>${escapeHtml(monthTitle($('#month').value))} · Los límites se definen por categoría.</p><button class="primary" data-action="new-budget">＋ Presupuesto</button></div><div class="cards-grid">${budgets.map(b=>`<article class="panel">${budgetBlock(b,true)}</article>`).join('')}</div>${budgets.length?'':`<article class="panel">${blank('Planea tu mes','Crea un presupuesto para alimentación, transporte u otra categoría.')}</article>`}`;
}
function renderGoals() {
    $('#page-goals').innerHTML=`<div class="section-toolbar"><p>${state.goals.length} metas personales</p><button class="primary" data-action="new-goal">＋ Nueva meta</button></div><div class="info-strip">El ahorro reservado de cada meta se registra manualmente. No modifica tu saldo ni genera ingresos o gastos; es una referencia para tus planes.</div><div class="cards-grid">${state.goals.map(g=>{const percent=Math.round(Number(g.saved)/Number(g.target)*100);return `<article class="panel goal-card"><span class="goal-symbol">◎</span><h2>${escapeHtml(g.name)}</h2><p class="muted" style="font-size:13px;margin-top:5px">Hasta el ${prettyDate(g.deadline)}</p><div class="goal-amount">${money(g.saved)} <span>de ${money(g.target)}</span></div><div class="progress" role="progressbar" aria-label="Avance de ${escapeHtml(g.name)}" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${percent}"><span style="width:${percent}%"></span></div><div class="budget-bottom"><span>${percent===100?'¡Meta alcanzada!':percent+'% de tu meta'}</span><span>${money(Number(g.target)-Number(g.saved))} por ahorrar</span></div><div class="card-actions"><button class="secondary" data-action="edit-goal" data-id="${g.id}">Actualizar ahorro</button><button class="quiet" data-action="delete-goals" data-id="${g.id}" aria-label="Eliminar meta">×</button></div></article>`;}).join('')}</div>${state.goals.length?'':`<article class="panel">${blank('¿Qué te gustaría lograr?','Una laptop, un viaje o tu fondo de emergencia. Empieza con una meta.')}</article>`}`;
}
function renderReports() {
    const t=totals(monthly());
    $('#page-reports').innerHTML=`<div class="info-strip">Periodo: <strong>${escapeHtml(monthTitle($('#month').value))}</strong> · Los reportes contienen únicamente los datos de tu cuenta.</div><div class="cards-grid">${[['movements','⇄','Movimientos del mes','El detalle de cada ingreso y gasto, con fecha, categoría y monto.'],['categories','◔','Gastos por categoría','Una vista agrupada para identificar dónde se concentra tu gasto.'],['summary','▤','Resumen financiero','Ingresos, gastos y balance del mes en un documento breve.']].map(([type,icon,title,description])=>`<article class="panel report-card"><span class="goal-symbol">${icon}</span><h2>${title}</h2><p>${description}</p><button class="secondary" data-action="download" data-type="${type}">↓ Descargar PDF</button></article>`).join('')}</div><article class="panel" style="margin-top:24px"><div class="panel-head"><h2>Antes de descargar</h2></div><div class="stats-grid" style="margin-bottom:0">${stat('Ingresos',t.income,'Mes seleccionado','↓')}${stat('Gastos',t.expense,'Mes seleccionado','↑')}${stat('Balance',t.balance,'Ingresos menos gastos','◈')}</div></article>`;
}
function renderCategories() {
    $('#page-categories').innerHTML=`<div class="section-toolbar"><p>Las categorías son privadas para tu cuenta.</p><button class="primary" data-action="new-category">＋ Categoría</button></div><div class="category-columns">${['GASTO','INGRESO'].map(kind=>`<article class="panel"><div class="panel-head"><h2>${kind==='GASTO'?'Gastos':'Ingresos'}</h2><span class="badge">${state.categories.filter(c=>c.kind===kind).length}</span></div><div class="category-list">${state.categories.filter(c=>c.kind===kind).map(c=>`<div class="category-row"><span>${escapeHtml(c.name)}</span><button class="quiet" data-action="delete-categories" data-id="${c.id}" aria-label="Eliminar ${escapeHtml(c.name)}">×</button></div>`).join('')||blank('Sin categorías','Añade una categoría para empezar.')}</div></article>`).join('')}</div><p class="muted" style="font-size:13px;margin-top:20px">No se pueden eliminar categorías que tengan movimientos o presupuestos asociados.</p>`;
}
async function renderAdmin() {
    $('#page-admin').innerHTML='<article class="panel"><p class="muted">Cargando cuentas…</p></article>';
    try {
        adminAccounts=await api('/admin/users');if(page!=='admin')return;
        $('#page-admin').innerHTML=`<div class="stats-grid">${[['Cuentas registradas',adminAccounts.length],['Cuentas activas',adminAccounts.filter(u=>u.enabled).length],['Cuentas desactivadas',adminAccounts.filter(u=>!u.enabled).length]].map(([label,count])=>`<article class="stat"><div class="stat-top">${label}</div><div class="stat-value">${count}</div></article>`).join('')}</div><article class="panel"><div class="panel-head"><h2>Acceso de usuarios</h2></div><div class="table-scroll"><table><thead><tr><th>NOMBRE</th><th>CORREO</th><th>ROL</th><th>ESTADO</th><th>ACCIÓN</th></tr></thead><tbody>${adminAccounts.map(u=>`<tr><td>${escapeHtml(u.name)}</td><td>${escapeHtml(u.email)}</td><td>${u.role==='ADMIN'?'Administrador':'Usuario'}</td><td><span class="badge ${u.enabled?'':'expense'}">${u.enabled?'Activo':'Desactivado'}</span></td><td>${u.role==='ADMIN'?'<span class="muted">Protegido</span>':`<button class="secondary" data-action="account-status" data-id="${u.id}">${u.enabled?'Desactivar':'Activar'}</button>`}</td></tr>`).join('')}</tbody></table></div></article><p class="muted" style="font-size:13px;margin-top:18px">Desactivar impide el acceso a la cuenta sin borrar sus registros. Las cuentas administradoras se gestionan desde la configuración local.</p>`;
    } catch(error) {if(page==='admin')$('#page-admin').innerHTML=blank('No se pudieron cargar las cuentas',error.message);}
}
function categoryOptions(kind,selected) {return state.categories.filter(c=>!kind||c.kind===kind).map(c=>`<option value="${c.id}" ${c.id===selected?'selected':''}>${escapeHtml(c.name)}</option>`).join('');}
function field(label,name,type,value='',extra='') {return `<label>${label}<input name="${name}" type="${type}" value="${escapeHtml(value)}" ${extra} required></label>`;}
function openEditor(type,id=null) {
    editContext={type,id};let html='',title='';
    if(type==='movement') {
        const m=id?state.movements.find(x=>x.id===id):null,kind=m?.kind||'GASTO';title=id?'Editar movimiento':'Nuevo movimiento';
        html=`<div class="field-grid"><label>Tipo<select name="kind"><option value="GASTO" ${kind==='GASTO'?'selected':''}>Gasto</option><option value="INGRESO" ${kind==='INGRESO'?'selected':''}>Ingreso</option></select></label>${field('Monto (S/)','amount','number',m?.amount||'','min="0.01" max="9999999999.99" step="0.01"')}</div>${field('Descripción','description','text',m?.description||'','maxlength="160" placeholder="Por ejemplo, compra del supermercado"')}<label>Categoría<select name="categoryId" required>${categoryOptions(kind,m?.categoryId)}</select></label><div class="field-grid">${field('Fecha','date','date',m?.date||today(),`min="1900-01-01" max="${today()}"`)}<label>Método de pago<select name="paymentMethod">${['Efectivo','Tarjeta','Yape','Plin','Transferencia'].map(v=>`<option ${m?.paymentMethod===v?'selected':''}>${v}</option>`).join('')}</select></label></div>`;
    } else if(type==='budget') {
        const b=id?state.budgets.find(x=>x.id===id):null;title=id?'Cambiar presupuesto':'Nuevo presupuesto';
        html=`<p class="form-hint">Si la categoría ya tiene un presupuesto en ese mes, se actualizará su límite.</p><label>Categoría de gasto<select name="categoryId" required>${categoryOptions('GASTO',b?.categoryId)}</select></label>${field('Mes','month','month',b?.month||$('#month').value)}${field('Límite mensual (S/)','amount','number',b?.amount||'','min="0.01" max="9999999999.99" step="0.01"')}`;
    } else if(type==='goal') {
        const g=id?state.goals.find(x=>x.id===id):null;title=id?'Actualizar meta':'Nueva meta de ahorro';
        html=`${field('Nombre de tu meta','name','text',g?.name||'','maxlength="80" placeholder="Por ejemplo, mi próxima laptop"')}<div class="field-grid">${field('Objetivo (S/)','target','number',g?.target||'','min="0.01" max="9999999999.99" step="0.01"')}${field('Ya reservado (S/)','saved','number',g?.saved||0,'min="0" max="9999999999.99" step="0.01"')}</div>${field('Fecha objetivo','deadline','date',g?.deadline||today())}<p class="form-hint">Este monto es una referencia de ahorro reservado. No crea un movimiento ni cambia tu saldo.</p>`;
    } else if(type==='category') {
        title='Nueva categoría';html=`${field('Nombre','name','text','','maxlength="60" placeholder="Por ejemplo, mascotas"')}<label>Tipo<select name="kind"><option value="GASTO">Gasto</option><option value="INGRESO">Ingreso</option></select></label>`;
    }
    $('#dialog-title').textContent=title;$('#form-fields').innerHTML=html;$('#form-error').textContent='';
    if(type==='movement')$('#editor-form').elements.kind.addEventListener('change',e=>$('#editor-form').elements.categoryId.innerHTML=categoryOptions(e.target.value));
    $('#editor').showModal();
}
$('#close-dialog').addEventListener('click',()=>$('#editor').close());$('#cancel-dialog').addEventListener('click',()=>$('#editor').close());
$('#editor-form').addEventListener('submit',async event=> {
    event.preventDefault();const button=$('button[type=submit]',event.target);button.disabled=true;$('#form-error').textContent='';
    const input=Object.fromEntries(new FormData(event.target)),{type,id}=editContext;
    for(const key of ['categoryId','amount','target','saved'])if(key in input)input[key]=Number(input[key]);
    const endpoint={movement:'movements',budget:'budgets',goal:'goals',category:'categories'}[type];
    if(type==='movement')delete input.kind;
    try {
        if(type==='goal'&&input.saved>input.target)throw new Error('El ahorro reservado no puede superar el objetivo.');
        const edit=id&&['movement','goal'].includes(type);
        await api('/'+endpoint+(edit?'/'+id:''),edit?'PUT':'POST',input);$('#editor').close();await refresh();toast('Guardado. Tus números ya están actualizados.');
    } catch(error){$('#form-error').textContent=error.message;}finally{button.disabled=false;}
});
$('#main').addEventListener('click',async event=> {
    const button=event.target.closest('[data-action]');if(!button)return;
    const action=button.dataset.action,id=Number(button.dataset.id);
    if(action==='account-status') {
        const account=adminAccounts.find(u=>u.id===id);if(!account)return;
        return confirm(account.enabled?'¿Desactivar esta cuenta?':'¿Activar esta cuenta?',`${account.name} (${account.email}). Sus registros se conservarán.`,account.enabled?'Desactivar':'Activar',async()=> {await api('/admin/users/'+id,'PATCH',{enabled:!account.enabled});await renderAdmin();toast('Estado de la cuenta actualizado.');});
    }
    if(action.startsWith('new-'))return openEditor(action.slice(4));
    if(action.startsWith('edit-'))return openEditor(action.slice(5),id);
    if(action.startsWith('delete-'))return confirm('¿Eliminar este registro?','Esta acción no se puede deshacer. Tus totales se actualizarán al eliminarlo.','Eliminar',async()=> {await api('/'+action.slice(7)+'/'+id,'DELETE');await refresh();toast('Registro eliminado.');});
    if(action==='prev'||action==='next'){filters.index+=action==='next'?1:-1;return renderMovementResults();}
    if(action==='download') {
        button.disabled=true;
        try {
            const response=await fetch('/api/reports?'+new URLSearchParams({month:$('#month').value,type:button.dataset.type}),{credentials:'same-origin'});
            if(response.status===401){showAuth();throw new Error('Vuelve a iniciar sesión para descargar el reporte.');}
            if(!response.ok)throw new Error('No se pudo generar el PDF. Inténtalo de nuevo.');
            const blob=await response.blob(),url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=`brujula-${button.dataset.type}-${$('#month').value}.pdf`;document.body.appendChild(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),10000);toast('Tu reporte está listo.');
        }catch(error){toast(error.message);}finally{button.disabled=false;}
    }
});
(async()=> {
    try {await getCsrf();const response=await fetch('/api/state',{credentials:'same-origin',cache:'no-store'});if(response.ok){state=await response.json();$('#loading').classList.add('hidden');$('#application').classList.remove('hidden');$('#header-name').textContent=state.user.name;$('#avatar').textContent=state.user.name.slice(0,2).toUpperCase();render();}else if(response.status===401)showAuth();else throw new Error('No se pudo cargar tu espacio. Recarga la página.');}
    catch(error){showAuth();$('#auth-error').textContent=error.message;}
})();
