import { fileURLToPath } from 'node:url';
process.chdir(fileURLToPath(new URL('../../', import.meta.url)));
import fs from 'node:fs';import path from 'node:path';import * as prettier from '../../frontend/node_modules/prettier/index.mjs';import java from '../../frontend/node_modules/prettier-plugin-java/dist/index.mjs';
const walk=p=>fs.readdirSync(p,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(p,e.name)):[path.join(p,e.name)]);
for(const p of [...walk('backend/src/main/java/com/learnflow/backend'),...walk('backend/src/test/java/com/learnflow/backend')]){if(!p.endsWith('.java')||p.includes('prediction')||['AIProvider.java','AiPrediction.java','RemoteAIProvider.java','AiPredictionTest.java'].includes(path.basename(p)))continue;fs.writeFileSync(p,await prettier.format(fs.readFileSync(p,'utf8'),{parser:'java',plugins:[java],tabWidth:4,printWidth:110}));}
for(const p of [...walk('frontend/src'),...['frontend/package.json','frontend/tsconfig.json','frontend/vite.config.ts','frontend/eslint.config.js','frontend/index.html']]){if(!/\.(tsx?|css|json|js|html)$/.test(p))continue;fs.writeFileSync(p,await prettier.format(fs.readFileSync(p,'utf8'),{filepath:p,singleQuote:true,printWidth:100}));}
console.log('Formatted active Java and React source files.');

