package com.signalcollect.triplerush

trait Optimizer {
	def optimize(cardinalities: Map[TriplePattern, Int]): List[TriplePattern]
}