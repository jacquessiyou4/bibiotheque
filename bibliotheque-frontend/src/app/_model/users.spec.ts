import { Users } from './users';

describe('Users', () => {
  it('should create an instance', () => {
    expect(new Users()).toBeTruthy();
  });

  it('should accept field assignment', () => {
    const user = new Users();
    user.userId = 1;
    user.username = 'john';
    user.name = 'John Doe';
    user.password = 'secret123';
    user.role = [{ roleName: 'ADHERENT' }];

    expect(user.userId).toBe(1);
    expect(user.username).toBe('john');
    expect(user.name).toBe('John Doe');
    expect(user.password).toBe('secret123');
    expect(user.role).toEqual([{ roleName: 'ADHERENT' }]);
  });

  it('should serialize from JSON', () => {
    const json = {
      userId: 42,
      username: 'jane',
      name: 'Jane Smith',
      password: 'pass',
      role: [{ roleName: 'BIBLIOTHECAIRE' }]
    };
    const user: Users = Object.assign(new Users(), json);

    expect(user.userId).toBe(42);
    expect(user.username).toBe('jane');
    expect(user.name).toBe('Jane Smith');
    expect(user.role).toEqual([{ roleName: 'BIBLIOTHECAIRE' }]);
  });
});
