import type { User } from "~/types/auth";

export const useUserStore = defineStore(
  "user",
  () => {
    const user = ref<User | null>(null);
    const setUser = (profile: User | undefined) => {
      if (profile) {
        user.value = profile;
      }
    };
    const initials = computed(() => {
      return (
        user.value?.fullName
          ?.trim()
          .split(/\s+/)
          .slice(0, 2)
          .map((word) => word.charAt(0).toUpperCase())
          .join("") ?? ""
      );
    });
    return { user, setUser, initials };
  },
  {
    persist: {
      storage: piniaPluginPersistedstate.cookies(),
      pick: ["user"],
    },
  },
);
