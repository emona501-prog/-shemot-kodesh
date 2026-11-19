
/* ---- אזהרת קדושה: מסך כניסה + פס קבוע ---- */
(function(){
  var w=document.getElementById('kd-warn'),btn=document.getElementById('kd-ok'),
      num=document.getElementById('kd-num'),bar=document.getElementById('kd-bar');
  if(!w||!btn||!bar) return;
  var SECS=7,locked=true,started=false;
  function setH(){document.documentElement.style.setProperty('--kdh',bar.offsetHeight+'px');}
  setH();addEventListener('resize',setH);setTimeout(setH,700);
  if(document.fonts&&document.fonts.ready) document.fonts.ready.then(setH);
  function tick(n){
    if(n<=0){
      locked=false;btn.disabled=false;
      btn.textContent='קראתי — אנהג בקדושה ובזהירות';
      num.textContent='✓';w.classList.add('kd-ready');return;
    }
    num.textContent=n;btn.textContent='נא לקרוא בעיון… ('+n+')';
    setTimeout(function(){tick(n-1);},1000);
  }
  window.kdWarnStart=function(){if(started)return;started=true;tick(SECS);};
  window.kdWarnClose=function(){if(locked)return;w.classList.remove('kd-show');document.documentElement.style.overflow='';};
  window.kdWarnOpen=function(){w.classList.add('kd-show');w.scrollTop=0;document.documentElement.style.overflow='hidden';if(!started)window.kdWarnStart();};
  document.documentElement.style.overflow='hidden';
  var spl=document.getElementById('spl');
  if(!spl||spl.style.display==='none'||spl.classList.contains('spl-off')) window.kdWarnStart();
})();
