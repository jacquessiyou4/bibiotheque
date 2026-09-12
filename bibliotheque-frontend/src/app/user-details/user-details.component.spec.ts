import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { UserDetailsComponent } from './user-details.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';
import { UsersService } from '../_service/users.service';
import { BorrowService } from '../_service/borrow.service';
import { Users } from '../_model/users';
import { Borrow } from '../_model/borrow';

describe('UserDetailsComponent', () => {
  let component: UserDetailsComponent;
  let fixture: ComponentFixture<UserDetailsComponent>;
  let usersServiceSpy: jasmine.SpyObj<UsersService>;
  let borrowServiceSpy: jasmine.SpyObj<BorrowService>;

  const mockUser: Users = {
    userId: 1,
    username: 'john',
    name: 'John Doe',
    password: '',
    role: [{ roleName: 'ADHERENT' }]
  };

  const mockBorrows: Borrow[] = [
    { borrowId: 10, bookId: 3, userId: 1, issueDate: new Date(), returnDate: null, dueDate: new Date() },
    { borrowId: 11, bookId: 5, userId: 1, issueDate: new Date(), returnDate: new Date(), dueDate: new Date() },
  ];

  beforeEach(async () => {
    usersServiceSpy = jasmine.createSpyObj('UsersService', ['getUserById']);
    borrowServiceSpy = jasmine.createSpyObj('BorrowService', ['getBooksBorrowedByUser']);

    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [ UserDetailsComponent, TranslatePipe ],
      providers: [
        { provide: ActivatedRoute, useValue: { snapshot: { params: { userId: '1' } } } },
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
        { provide: UsersService, useValue: usersServiceSpy },
        { provide: BorrowService, useValue: borrowServiceSpy },
      ]
    })
    .compileComponents();

    usersServiceSpy.getUserById.and.returnValue(of(mockUser));
    borrowServiceSpy.getBooksBorrowedByUser.and.returnValue(of(mockBorrows));

    fixture = TestBed.createComponent(UserDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('ngOnInit charge l\u2019utilisateur depuis le service', () => {
    expect(usersServiceSpy.getUserById).toHaveBeenCalledWith(1);
    expect(component.user.username).toBe('john');
    expect(component.user.name).toBe('John Doe');
  });

  it('ngOnInit charge l\u2019historique des emprunts de l\u2019utilisateur', () => {
    expect(borrowServiceSpy.getBooksBorrowedByUser).toHaveBeenCalledWith(1);
    expect(component.borrow.length).toBe(2);
    expect(component.borrow[0].borrowId).toBe(10);
  });
});
