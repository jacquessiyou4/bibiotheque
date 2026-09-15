import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from './pipes/translate.pipe';

/**
 * Déclarables communs à toutes les fonctionnalités : importé par chaque
 * module de fonctionnalité au lieu de redéclarer le pipe de traduction.
 */
@NgModule({
  declarations: [TranslatePipe],
  exports: [CommonModule, FormsModule, RouterModule, TranslatePipe]
})
export class SharedModule { }
