import { Routes } from '@angular/router';
import { unsavedChangesGuard } from '../../shared/guards/unsaved-changes-guard';
import { CoursesTab } from './student-detail-page/courses-tab';
import { EnrollTab } from './student-detail-page/enroll-tab';
import { ProfileTab } from './student-detail-page/profile-tab';
import { StudentDetailPage } from './student-detail-page/student-detail-page';
import { StudentEditorPage } from './student-editor-page/student-editor-page';
import { StudentListPage } from './student-list-page/student-list-page';

export default [
  { path: '', title: 'Students', component: StudentListPage },
  {
    path: 'new',
    title: 'New student',
    component: StudentEditorPage,
    canDeactivate: [unsavedChangesGuard],
  },
  {
    path: ':id/edit',
    title: 'Edit student',
    component: StudentEditorPage,
    canDeactivate: [unsavedChangesGuard],
  },
  {
    path: ':id',
    title: 'Student',
    component: StudentDetailPage,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'profile' },
      { path: 'profile', component: ProfileTab, canDeactivate: [unsavedChangesGuard] },
      { path: 'courses', component: CoursesTab },
      { path: 'enroll', component: EnrollTab, canDeactivate: [unsavedChangesGuard] },
    ],
  },
] satisfies Routes;
