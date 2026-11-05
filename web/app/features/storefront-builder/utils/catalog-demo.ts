import type {InjectionKey} from 'vue';
import type {Product} from '~/types/product';
import type {CollectionSummary} from '~/types/collection';
export interface ThemeDemoCatalog {products:Product[];collections:CollectionSummary[]}
export const themeDemoCatalogKey:InjectionKey<ThemeDemoCatalog>=Symbol('theme-demo-catalog');
