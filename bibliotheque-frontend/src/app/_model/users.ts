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

/**
 * Utilisateur tel que manipulé par les composants (rôles = [{ roleName }]).
 * Pas de champ mot de passe : le backend ne l'envoie jamais, et un nouveau
 * mot de passe se transmet à part (CreateUserRequest, UsersService.updateUser).
 */
export class Users {
    userId: number;
    username: string;
    name: string;
    role: { roleName: string }[];
}
