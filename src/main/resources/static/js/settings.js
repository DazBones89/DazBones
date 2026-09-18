document.querySelectorAll('.auto-setting').forEach(form=>{
 const box=form.querySelector('[name=visible]'),output=form.querySelector('output');
 box.addEventListener('change',async()=>{box.disabled=true;output.textContent='保存中…';
 try{const data=new FormData(form);data.set('visible',String(box.checked));const response=await fetch(form.action,{method:'POST',body:data});if(!response.ok)throw Error();output.textContent='保存済み';}
 catch{box.checked=!box.checked;output.textContent='保存失敗。もう一度変更してください。';}finally{box.disabled=false;}});
});
