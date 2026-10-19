import { fileURLToPath } from 'node:url';
process.chdir(fileURLToPath(new URL('../../', import.meta.url)));
// Download the current photographed color variants from their recorded public source URLs.
import fs from 'node:fs';
import path from 'node:path';
const manifest=JSON.parse(fs.readFileSync('frontend/public/assets/photos/colors.json','utf8'));
const directory=path.resolve('frontend/public/assets/photos');
for(const product of manifest.products)for(const color of product.colors){
  const destination=path.resolve('frontend/public',color.imageUrl.slice(1));
  if(!destination.startsWith(directory+path.sep))throw Error('Invalid asset destination');
  const r=await fetch(color.sourceUrl);if(!r.ok)throw Error('Photo download failed: '+r.status);
  const data=Buffer.from(await r.arrayBuffer());
  if(data.length<5000)throw Error('Invalid photo data');
  fs.writeFileSync(destination,data);
  console.log(product.productId,color.color,path.basename(destination));
}
