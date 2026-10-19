import { fileURLToPath } from 'node:url';
process.chdir(fileURLToPath(new URL('../../', import.meta.url)));
import fs from 'node:fs';
const html=fs.readFileSync('project-data/.local/reference/cardina.html','utf8');
const photos=[...html.matchAll(/<img[^>]*class="bclazy img-loop image-m"[^>]*data-src="([^"]+)"[^>]*alt="([^"]*)"[^>]*>/g)].map(m=>({url:('https:'+m[1]).replace('_grande.','.'),alt:m[2].trim()}));
const categories=[...html.matchAll(/https:\/\/cdn.hstatic.net\/products\/[^"<>\s]+_master\.jpg/g)].map(m=>m[0]);
const items=[
 {file:'reference-banner.jpg',url:'https://cdn.hstatic.net/files/200000503583/file/khuyen-mai-20-10_b37fa6dd4d46408d8754bebd681b2c5e.jpg'},
 {file:'reference-knitwear.jpg',url:'https://cdn.hstatic.net/files/200000503583/file/2000x840_b43e9106d7f24ce085eca1ff979970a0.jpg'},
 {file:'product-1.jpg',url:categories.find(s=>s.includes('ao-tho-gio-co-nhun'))},
 {file:'product-2.jpg',...photos.find(p=>p.url.includes('quan-da-cong-so'))},
 {file:'product-3.jpg',url:categories.find(s=>s.includes('dam-so-mi'))},
 {file:'product-4.jpg',...photos.find(p=>p.url.includes('ao0-thun-tam'))},
 {file:'product-5.jpg',url:categories.find(s=>s.includes('quan-jean'))},
 {file:'product-6.jpg',url:'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=1000&q=85',alt:'Balo thời trang'},
 {file:'product-7.jpg',...photos.find(p=>p.url.includes('ao-ni-dai-co-leo'))},
 {file:'product-8.jpg',...photos.find(p=>p.url.includes('quan-sooc-da-dang-a'))},
 {file:'category-set.jpg',url:categories.find(s=>s.includes('do-bo__'))},
 {file:'category-knit.jpg',url:categories.find(s=>s.includes('ao-thun-det-hoa-tiet'))},
];
fs.mkdirSync('frontend/public/assets/photos',{recursive:true});
await Promise.all(items.map(async item=>{if(!item.url)throw Error('Missing URL for '+item.file);const r=await fetch(item.url);if(!r.ok)throw Error(item.file+': '+r.status);const data=Buffer.from(await r.arrayBuffer());if(data.length<10000)throw Error('Image too small: '+item.file);fs.writeFileSync('frontend/public/assets/photos/'+item.file,data);console.log(item.file+': '+Math.round(data.length/1024)+' KB');}));
fs.writeFileSync('frontend/public/assets/photos/sources.json',JSON.stringify({retrieved:'2026-10-18',reference:'https://cardina.vn/',images:items},null,2));

