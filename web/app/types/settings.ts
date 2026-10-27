export interface Setting {
  id: string
  key: string
  value: unknown
  createdAt: string
  updatedAt: string
}

export interface SettingCommand {
  key: string
  value: Record<string, unknown> | unknown[];
}
