import type { Category, CategoryTreeNode } from "~/types/category";

export const buildCategoryTree = (
  categories: Category[],
): CategoryTreeNode[] => {
  const nodes = new Map<string, CategoryTreeNode>();

  for (const category of categories) {
    nodes.set(category.id, {
      ...category,
      children: [],
    });
  }

  const roots: CategoryTreeNode[] = [];

  for (const category of categories) {
    const node = nodes.get(category.id)!;

    if (!category.parentId) {
      roots.push(node);
      continue;
    }

    const parent = nodes.get(category.parentId);

    if (parent) {
      parent.children.push(node);
    }
  }

  return roots;
};