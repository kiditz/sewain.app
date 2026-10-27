import type {
  ChangePasswordCommand,
  UpdateProfileCommand,
  User,
} from "~/types/auth";

export const useProfile = () => {
  const api = useApi();
  const findProfile = async () => {
    return await api<User>("/profile");
  };
  const updateProfile = async (command: UpdateProfileCommand) => {
    return await api<User>("/profile", {
      method: "PUT",
      body: command,
    });
  };
  const changePassword = async (command: ChangePasswordCommand) => {
    return await api("/profile/password", {
      method: "PUT",
      body: command,
    });
  };
  return { findProfile, updateProfile, changePassword };
};
