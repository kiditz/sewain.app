export const blockFieldGroups = ['content','typography','links','style','padding','layout','visibility'] as const;
export type BlockFieldGroup = typeof blockFieldGroups[number];

/** Presentation only: saved setting keys and update payloads remain unchanged. */
export function blockFieldGroup(key:string):BlockFieldGroup {
  if(['level','fontSize','fontFamily','fontWeight','lineHeight','letterSpacing'].includes(key))return 'typography';
  if(/(?:image|media|logo|favicon|poster|thumbnail)(?:url|src|source)?$/i.test(key))return 'content';
  if(['url','href','link','linkTargets','menuId','openInNewTab','target','action','__socialLink'].includes(key)||/(?:url|href|link|linkTo|destination|redirectTo|route|path)$/i.test(key))return 'links';
  if(['style','appearance','borderRadius','color','backgroundColor','border','shadow','opacity','thickness'].includes(key))return 'style';
  if(key==='padding')return 'padding';
  if(['layout','width','height','size','aspectRatio','columns','mobileColumns','itemsGap','displayType','x','y'].includes(key))return 'layout';
  if(key==='__visibility')return 'visibility';
  return 'content';
}
