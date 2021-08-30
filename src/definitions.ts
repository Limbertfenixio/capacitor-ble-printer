export interface ZPLConverterPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
