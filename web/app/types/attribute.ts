export interface Attribute {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  values: AttributeValue[];
}

export interface AttributeValue {
  id: string;
  value: string;
  slug: string;
}

export interface AttributeCommand {
  name: string;
  slug: string;
  description: string | null;
  values: AttributeValueCommand[];
}

export interface AttributeValueCommand {
  id?: string;
  value: string;
  slug: string;
}
