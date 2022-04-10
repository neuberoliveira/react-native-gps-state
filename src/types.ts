export type PermissionType = number;
export type PermissionState = {
  NOT_DETERMINED: PermissionType;
  RESTRICTED: PermissionType;
  DENIED: PermissionType;
  AUTHORIZED: PermissionType;
  AUTHORIZED_ALWAYS: PermissionType;
  AUTHORIZED_WHENINUSE: PermissionType;
};

export type ListenerCallback = (status: PermissionType) => void;
