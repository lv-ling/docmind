<script setup lang="ts">
import {
  Activity,
  AlertTriangle,
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  ArrowUp,
  Bell,
  Bold,
  Bot,
  BrainCircuit,
  Building2,
  Check,
  CheckCircle2,
  CheckSquare,
  ChevronLeft,
  ChevronRight,
  ChevronsUpDown,
  Circle,
  CircleAlert,
  CircleCheck,
  CircleX,
  Clock,
  Cpu,
  File,
  FileSearch,
  FileWarning,
  Files,
  FileText,
  Filter,
  GitCompare,
  Info,
  Layers,
  LayoutGrid,
  LayoutList,
  Lightbulb,
  ListChecks,
  Loader2,
  LoaderCircle,
  LogOut,
  Menu,
  Play,
  Plus,
  RefreshCw,
  Receipt,
  Save,
  ScanLine,
  Search,
  Settings,
  ShieldAlert,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  TextQuote,
  TrendingUp,
  Upload,
  UserCheck,
  Users,
  Eye,
  EyeOff,
  X,
  Zap,
} from 'lucide-vue-next';
import { computed } from 'vue';

const iconComponents = {
  activity: Activity,
  alert: ShieldAlert,
  'alert-triangle': AlertTriangle,
  'arrow-down': ArrowDown,
  arrow: ArrowRight,
  'arrow-left': ArrowLeft,
  'arrow-right': ArrowRight,
  'arrow-up': ArrowUp,
  bell: Bell,
  bold: Bold,
  bot: Bot,
  'brain-circuit': BrainCircuit,
  'building-2': Building2,
  check: Check,
  'check-circle-2': CheckCircle2,
  'check-square': CheckSquare,
  'chevron-left': ChevronLeft,
  'chevron-right': ChevronRight,
  chevrons: ChevronsUpDown,
  'chevrons-up-down': ChevronsUpDown,
  circle: Circle,
  'circle-alert': CircleAlert,
  'circle-check': CircleCheck,
  'circle-x': CircleX,
  clock: Clock,
  close: X,
  cpu: Cpu,
  document: FileText,
  documents: Files,
  file: File,
  'file-search': FileSearch,
  'file-text': FileText,
  'file-warning': FileWarning,
  files: Files,
  filter: Filter,
  'git-compare': GitCompare,
  grid: LayoutGrid,
  layers: Layers,
  'layout-list': LayoutList,
  'layout-grid': LayoutGrid,
  lightbulb: Lightbulb,
  'list-checks': ListChecks,
  'loader-2': Loader2,
  'loader-circle': LoaderCircle,
  logout: LogOut,
  menu: Menu,
  play: Play,
  plus: Plus,
  refresh: RefreshCw,
  receipt: Receipt,
  review: CheckSquare,
  'scan-line': ScanLine,
  schema: SlidersHorizontal,
  search: Search,
  save: Save,
  settings: Settings,
  'shield-alert': ShieldAlert,
  'shield-check': ShieldCheck,
  'sliders-horizontal': SlidersHorizontal,
  sparkles: Sparkles,
  'text-quote': TextQuote,
  template: Layers,
  'trending-up': TrendingUp,
  upload: Upload,
  'user-check': UserCheck,
  users: Users,
  eye: Eye,
  'eye-off': EyeOff,
  info: Info,
  zap: Zap,
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
  <span
    class="app-icon inline-flex shrink-0 items-center justify-center align-middle leading-none"
    :aria-hidden="ariaHidden"
    :aria-label="label"
    :role="label === undefined ? undefined : 'img'"
  >
    <component
      :is="iconComponent"
      class="app-icon__svg block size-full shrink-0"
      :stroke-width="1.8"
      aria-hidden="true"
      focusable="false"
    />
  </span>
</template>
