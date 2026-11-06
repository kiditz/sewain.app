import sanitizeHtml from 'sanitize-html';

export function safeNavigationUrl(input:unknown):string {
  if(typeof input!=='string')return '';
  const value=input.trim();
  if(!value||/[\u0000-\u0020\u007f\\]/.test(value))return '';
  if(value.startsWith('/')&&!value.startsWith('//'))return value;
  if(value.startsWith('#')||value.startsWith('?'))return value;
  try{const url=new URL(value);return ['http:','https:'].includes(url.protocol)&&url.host?url.href:'';}catch{return '';}
}
export function sanitizeRichText(input:unknown):string {
  return sanitizeHtml(typeof input==='string'?input:'',{
    allowedTags:['p','h1','h2','h3','h4','h5','h6','strong','b','em','i','u','s','ol','ul','li','blockquote','pre','code','br','span','a'],
    allowedAttributes:{'*':['class'],li:[{name:'data-list',values:['ordered','bullet']}],a:['href','title']},
    allowedClasses:{'*':[/^ql-(?:align-(?:center|right|justify)|indent-[1-8]|direction-rtl|syntax|code-block|code-block-container|ui)$/]},
    allowedSchemes:['http','https'],allowProtocolRelative:false,parseStyleAttributes:false,
    transformTags:{a:(tagName,attribs)=>{
      const href=safeNavigationUrl(attribs.href);
      const {href:ignored,...rest}=attribs;
      return {tagName,attribs:{...rest,...(href?{href}:{})}};
    }},
  });
}
