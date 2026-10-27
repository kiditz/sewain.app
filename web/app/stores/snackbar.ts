import { defineStore } from "pinia";

export type SnackbarType = "success" | "error" | "info" | "warning";

export const useSnackbarStore = defineStore("snackbar", () => {
  const show = ref(false);
  const message = ref("");
  const header = ref("");
  const color = ref("primary");
  const type = ref<SnackbarType>("info");
  const timeout = ref(10000);

  const openSnackbar = (options: {
    message: string;
    header?: string;
    type?: SnackbarType;
    timeout?: number;
  }) => {
    message.value = options.message;
    header.value = options.header ?? "";
    type.value = options.type ?? "info";
    timeout.value = options.timeout ?? 4000;

    color.value = {
      success: "success",
      error: "error",
      info: "info",
      warning: "warning",
    }[type.value];

    show.value = true;
  };

  const hideSnackbar = () => {
    show.value = false;
  };

  return {
    show,
    message,
    header,
    color,
    type,
    timeout,
    openSnackbar,
    hideSnackbar,
  };
});
