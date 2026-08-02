/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue';

  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>;
  export default component;
}

interface OnlyOfficeEditorInstance {
  destroyEditor: () => void;
}

interface OnlyOfficeDocsApi {
  DocEditor: new (
    placeholderId: string,
    config: Record<string, unknown>,
  ) => OnlyOfficeEditorInstance;
}

interface Window {
  DocsAPI?: OnlyOfficeDocsApi;
}
