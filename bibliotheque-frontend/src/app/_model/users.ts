export interface UserListItem {
    userId: number;
    username: string;
    name: string;
    roles: string[];
}

export interface CreateUserRequest {
    username: string;
    name: string;
    password: string;
    roles: string[];
}

// Legacy class for backward compatibility - prefer using the interfaces above
export class Users {
    userId: number;
    username: string;
    name: string;
    password: string;
    role: { roleName: string }[];
}
