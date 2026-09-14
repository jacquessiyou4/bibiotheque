import { Users } from './users';

describe('Users', () => {
  it('devrait créer une instance', () => {
    expect(new Users()).toBeTruthy();
  });

  it('devrait accepter l\'assignation de champs', () => {
    const user = new Users();
    user.userId = 1;
    user.username = 'john';
    user.name = 'John Doe';
    user.role = [{ roleName: 'ADHERENT' }];

    expect(user.userId).toBe(1);
    expect(user.username).toBe('john');
    expect(user.name).toBe('John Doe');
    expect(user.role).toEqual([{ roleName: 'ADHERENT' }]);
  });

  it('devrait sérialiser depuis JSON (password omis par @JsonIgnore)', () => {
    const json = {
      userId: 42,
      username: 'jane',
      name: 'Jane Smith',
      role: [{ roleName: 'BIBLIOTHECAIRE' }]
    };
    const user: Users = Object.assign(new Users(), json);

    expect(user.userId).toBe(42);
    expect(user.username).toBe('jane');
    expect(user.name).toBe('Jane Smith');
    expect(user.role).toEqual([{ roleName: 'BIBLIOTHECAIRE' }]);
  });
});
