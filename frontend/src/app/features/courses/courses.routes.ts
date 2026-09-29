import { Routes } from '@angular/router';
import { unsavedChangesGuard } from '../../shared/guards/unsaved-changes-guard';
import { CourseEditorPage } from './course-editor-page/course-editor-page';
import { CourseListPage } from './course-list-page/course-list-page';

export default [
  { path: '', title: 'Courses', component: CourseListPage },
  {
    path: 'new',
    title: 'New course',
    component: CourseEditorPage,
    canDeactivate: [unsavedChangesGuard],
  },
  {
    path: ':id/edit',
    title: 'Edit course',
    component: CourseEditorPage,
    canDeactivate: [unsavedChangesGuard],
  },
] satisfies Routes;
