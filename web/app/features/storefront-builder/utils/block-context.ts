import type {ComputedRef,InjectionKey} from 'vue';
import type {CollectionSummary} from '~/types/collection';
export const collectionBlockContext:InjectionKey<ComputedRef<CollectionSummary|undefined>>=Symbol('collection-block-context');
export const embeddedPageContext:InjectionKey<ComputedRef<string[]>>=Symbol('embedded-page-context');
