import { fileURLToPath } from 'node:url';
process.chdir(fileURLToPath(new URL('../../', import.meta.url)));
// Run against the dedicated local test server (see README). No external services are called.
import assert from 'node:assert/strict';
const base=process.env.TEST_API_URL||'http://127.0.0.1:8081';
if(!['127.0.0.1','localhost'].includes(new URL(base).hostname))throw Error('Local test servers only');
async function call(path,token,body,method='POST'){
 const r=await fetch(base+'/api'+path,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body===undefined?undefined:JSON.stringify(body)});
 const text=await r.text();return {status:r.status,data:text?JSON.parse(text):null};
}
const admin=await call('/auth/login',null,{email:process.env.TEST_ADMIN_EMAIL||'admin@forme.local',password:process.env.TEST_ADMIN_PASSWORD||'FormeLocalAdmin2026!'});
assert.equal(admin.status,200);const at=admin.data.accessToken;const tag=Date.now().toString(36);
const customers=[];
const customerTokens=(process.env.TEST_CUSTOMER_TOKENS||'').split(',').filter(Boolean);
assert.equal(customerTokens.length,2,'Set TEST_CUSTOMER_TOKENS to two distinct verified customer JWTs from the dedicated test server. Email verification cannot be bypassed.');
for(const t of customerTokens){const address=await call('/addresses',t,{receiverName:'Smoke Customer',phoneNumber:'0901234567',province:'Ha Noi',detailAddress:'Local test address',isDefault:true});assert.equal(address.status,200);customers.push({t,addressId:address.data.addressId});}
const created=[];
async function product(stock,suffix){const r=await call('/admin/products',at,{categoryId:1,productName:'Concurrency fixture '+tag+' '+suffix,description:'Disposable local test fixture',brand:'TEST',material:'Cotton',gender:'Unisex',price:500000,salePrice:null,status:'Active',variants:[{size:'M',color:'Sand',stockQuantity:stock,sku:'SMOKE-'+tag+'-'+suffix}],images:['/assets/photos/product-1.jpg']});assert.equal(r.status,200);created.push(r.data.productId);return r.data;}
async function compete(p,couponCode=''){
 for(const c of customers){let cart=await call('/carts/current',c.t,undefined,'GET');for(const item of cart.data.items)await call('/carts/current/items/'+item.cartItemId,c.t,undefined,'DELETE');assert.equal((await call('/carts/current/items',c.t,{variantId:p.variants[0].variantId,quantity:1})).status,200);}
 return Promise.all(customers.map(c=>call('/orders',c.t,{addressId:c.addressId,paymentMethod:'COD',couponCode,note:'Concurrency smoke test'})));
}
try{
 const p=await product(1,'stock');const results=await compete(p);assert.deepEqual(results.map(r=>r.status).sort(),[200,400]);const fresh=await call('/products/'+p.productId,at,undefined,'GET');assert.equal(fresh.data.variants[0].stockQuantity,0);
 const winner=results.findIndex(r=>r.status===200);assert.equal((await call('/orders/'+results[winner].data.orderId+'/cancel',customers[winner].t,{})).status,200);assert.equal((await call('/products/'+p.productId,at,undefined,'GET')).data.variants[0].stockQuantity,1);
 const coupon='SMOKE_'+tag.toUpperCase();assert.equal((await call('/admin/coupons',at,{couponCode:coupon,discountType:'Fixed',discountValue:10000,minOrderValue:0,usageLimit:1,startDate:'2026-01-01T00:00:00Z',endDate:'2030-12-31T23:59:59Z'})).status,200);
 // Distinct products allow both transactions to reach the coupon lock concurrently.
 const a=await product(5,'coupon-a');const b=await product(5,'coupon-b');
 for(let n=0;n<2;n++){const c=customers[n];const cart=await call('/carts/current',c.t,undefined,'GET');for(const i of cart.data.items)await call('/carts/current/items/'+i.cartItemId,c.t,undefined,'DELETE');await call('/carts/current/items',c.t,{variantId:[a,b][n].variants[0].variantId,quantity:1});}
 const coupons=await Promise.all(customers.map(c=>call('/orders',c.t,{addressId:c.addressId,paymentMethod:'COD',couponCode:coupon,note:'Coupon concurrency smoke test'})));assert.deepEqual(coupons.map(r=>r.status).sort(),[200,400]);
 const response=await fetch(base+'/api/ai/chat/stream',{method:'POST',headers:{Authorization:'Bearer '+at,'Content-Type':'application/json'},body:JSON.stringify({message:'What is your shipping policy?'})});assert.equal(response.status,200);assert.match(await response.text(),/event:result/);
 console.log('PASS: stock contention, cancellation restock, coupon contention, authenticated SSE.');
}finally{for(const id of created)await call('/admin/products/'+id,at,undefined,'DELETE');}

