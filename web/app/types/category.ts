export interface Category {
  id: string;
  name: string;
  slug: string;
  imageUrl?: string | null;
  parentId: string | null;
}
export interface CategoryCommand {
  name: string;
  slug: string;
  imageUrl?: string | null;
  parentId: string | null;
}

export interface CategoryTreeNode extends Category {
  children: CategoryTreeNode[];
}
