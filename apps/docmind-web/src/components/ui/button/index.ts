import { cva } from 'class-variance-authority';

export { default as Button } from './Button.vue';

export const buttonVariants = cva(
  'inline-flex shrink-0 items-center justify-center whitespace-nowrap',
);
