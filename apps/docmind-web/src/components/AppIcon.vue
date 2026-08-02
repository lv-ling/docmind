<script setup lang="ts">
import {
  Activity,
  ArrowRight,
  Bell,
  Bot,
  BrainCircuit,
  CheckCircle2,
  CheckSquare,
  ChevronRight,
  ChevronsUpDown,
  Cpu,
  File,
  Files,
  FileText,
  Filter,
  Layers,
  LayoutGrid,
  Lightbulb,
  ListChecks,
  Loader2,
  LogOut,
  Play,
  Plus,
  RefreshCw,
  Search,
  Settings,
  ShieldAlert,
  SlidersHorizontal,
  Sparkles,
  TrendingUp,
  Upload,
  X,
} from 'lucide-vue-next';
import { computed } from 'vue';

const iconComponents = {
  activity: Activity,
  alert: ShieldAlert,
  arrow: ArrowRight,
  'arrow-right': ArrowRight,
  bell: Bell,
  bot: Bot,
  'brain-circuit': BrainCircuit,
  'check-circle-2': CheckCircle2,
  'check-square': CheckSquare,
  'chevron-right': ChevronRight,
  chevrons: ChevronsUpDown,
  'chevrons-up-down': ChevronsUpDown,
  close: X,
  cpu: Cpu,
  document: FileText,
  documents: Files,
  file: File,
  'file-text': FileText,
  files: Files,
  filter: Filter,
  grid: LayoutGrid,
  layers: Layers,
  'layout-grid': LayoutGrid,
  lightbulb: Lightbulb,
  'list-checks': ListChecks,
  'loader-2': Loader2,
  logout: LogOut,
  play: Play,
  plus: Plus,
  refresh: RefreshCw,
  review: CheckSquare,
  schema: SlidersHorizontal,
  search: Search,
  settings: Settings,
  'shield-alert': ShieldAlert,
  'sliders-horizontal': SlidersHorizontal,
  sparkles: Sparkles,
  template: Layers,
  'trending-up': TrendingUp,
  upload: Upload,
} as const;

type AppIconName = keyof typeof iconComponents;

const props = defineProps<{
  name: AppIconName;
  label?: string;
}>();

const iconComponent = computed(() => iconComponents[props.name]);
const ariaHidden = computed(() => (props.label === undefined ? 'true' : undefined));
</script>

<template>
  <component
    :is="iconComponent"
    class="app-icon"
    :stroke-width="1.8"
    :aria-hidden="ariaHidden"
    :aria-label="label"
    :role="label === undefined ? undefined : 'img'"
  />
</template>
