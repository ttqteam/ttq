package ttq.pipeline

import ttq.lf.Query
import ttq.tokenizer.Token

case class PipelineResult(tokens: List[Token], query: Query)
